import { router, Stack, useLocalSearchParams } from 'expo-router';
import { Send, Sparkles, Trash2 } from 'lucide-react-native';
import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ScholarlyText } from '@/components/ui/scholarly-text';
import { useAppTheme } from '@/components/ui/theme-provider';
import { apiClient, ApiError } from '@/lib/api-client';
import { formatRelativeTime } from '@/lib/format';
import { useAuthStore } from '@/store/auth-store';
import { useUserById } from '@/lib/user-cache';
import type { Message, MessageCreate, User } from '@/types';

const AI_CONVERSATION_ID = 'conv_ai';
const AI_SENDER_ID = 'ai_copilot';

const AI_COPILOT_USER: User = {
  id: AI_SENDER_ID,
  name: 'CiteCircle AI Copilot',
  avatar_url: '',
  role: 'RESEARCHER',
  institution: 'CiteCircle AI Core',
  field_of_study: '',
  bio: 'AI Academic Assistant',
  orcid_id: '',
  follower_count: 0,
  following_count: 0,
  citation_count: 0,
  external_citation_count: 0,
  publication_count: 0,
  orcid_verified: false,
  is_verified: true,
  interests: [],
};

const PROMPT_CHIPS = [
  'Review my abstract',
  'Explain P-value vs Effect Size',
  'Tips for literature review',
  'How to write research methodology',
];

export default function ChatScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors } = useAppTheme();
  const currentUser = useAuthStore((state) => state.user);
  const isAi = id === AI_CONVERSATION_ID;

  const [messages, setMessages] = useState<Message[]>([]);
  const [peerId, setPeerId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [messageText, setMessageText] = useState('');
  const [isSending, setIsSending] = useState(false);
  const scrollRef = useRef<ScrollView>(null);

  const resolvedPeer = useUserById(isAi ? null : peerId);
  const recipient = isAi ? AI_COPILOT_USER : resolvedPeer;

  const load = useCallback(async () => {
    const data = await apiClient.get<Message[]>(`/conversations/${id}/messages`);
    setMessages(data);
    if (!isAi) {
      const peer = data.find((m) => m.sender_id !== currentUser?.id)?.sender_id ?? null;
      setPeerId(peer);
    }
  }, [id, isAi, currentUser?.id]);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  useEffect(() => {
    scrollRef.current?.scrollToEnd({ animated: true });
  }, [messages.length, isSending]);

  async function sendMessage(content: string) {
    if (!content.trim() || isSending) return;
    setMessageText('');
    setIsSending(true);

    const optimistic: Message = {
      id: `optimistic_${Date.now()}`,
      conversation_id: id,
      sender_id: currentUser?.id ?? 'me',
      content: content.trim(),
      timestamp: Date.now(),
      is_read: true,
      attached_paper_id: null,
    };
    setMessages((current) => [...current, optimistic]);

    try {
      const body: MessageCreate = { content: content.trim() };
      const stored = await apiClient.post<Message[]>(`/conversations/${id}/messages`, body);
      setMessages((current) => [...current.filter((m) => m.id !== optimistic.id), ...stored]);
    } catch {
      setMessages((current) => current.filter((m) => m.id !== optimistic.id));
      setMessageText(content);
    } finally {
      setIsSending(false);
    }
  }

  function confirmClearHistory() {
    Alert.alert(
      'Clear Chat History?',
      isAi
        ? 'This will delete all saved messages in your AI Copilot conversation.'
        : 'This will delete all messages in this conversation.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Clear History',
          style: 'destructive',
          onPress: async () => {
            try {
              await apiClient.delete(`/conversations/${id}/messages`);
              setMessages([]);
            } catch (err) {
              Alert.alert('Could not clear history', err instanceof ApiError ? err.message : 'Please try again.');
            }
          },
        },
      ],
    );
  }

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView
      className="flex-1 bg-academic-paper dark:bg-[#15151A]"
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
    >
      <Stack.Screen
        options={{
          title: recipient?.name ?? 'Chat',
          headerRight: () => (
            <Pressable onPress={confirmClearHistory} hitSlop={8}>
              <Trash2 size={20} color={colors.textSecondary} />
            </Pressable>
          ),
        }}
      />

      <ScrollView ref={scrollRef} contentContainerClassName="gap-3 p-4">
        {messages.map((message) => (
          <MessageBubble key={message.id} message={message} isMine={message.sender_id === currentUser?.id} />
        ))}
        {isSending && isAi ? <TypingIndicator /> : null}
      </ScrollView>

      <SafeAreaView edges={['bottom']} className="border-t border-academic-gold/15 bg-white dark:border-[#33333D] dark:bg-[#1F1F26]">
        {isAi ? (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerClassName="gap-2 px-3 pt-2.5">
            {PROMPT_CHIPS.map((chip) => (
              <Pressable
                key={chip}
                onPress={() => sendMessage(chip)}
                className="rounded-full bg-academic-parchment px-3 py-1.5 dark:bg-[#2A2A33]"
              >
                <Text className="text-xs font-medium text-academic-ink dark:text-[#EFEAE0]">{chip}</Text>
              </Pressable>
            ))}
          </ScrollView>
        ) : null}

        <View className="flex-row items-center gap-2 p-3">
          <TextInput
            value={messageText}
            onChangeText={setMessageText}
            placeholder={isAi ? 'Ask CiteCircle AI Copilot…' : 'Type message…'}
            placeholderTextColor={colors.textSecondary}
            multiline
            className="max-h-24 flex-1 rounded-full border border-academic-gold/20 px-4 py-2.5 text-sm text-academic-ink dark:border-[#33333D] dark:text-[#EFEAE0]"
          />
          <Pressable
            onPress={() => sendMessage(messageText)}
            disabled={!messageText.trim() || isSending}
            className="h-10 w-10 items-center justify-center rounded-full bg-academic-maroon dark:bg-[#C6635E]"
            style={{ opacity: !messageText.trim() || isSending ? 0.5 : 1 }}
          >
            {isSending ? <ActivityIndicator size="small" color="#FBF9F4" /> : <Send size={16} color="#FBF9F4" />}
          </Pressable>
        </View>
      </SafeAreaView>
    </KeyboardAvoidingView>
  );
}

function MessageBubble({ message, isMine }: { message: Message; isMine: boolean }) {
  const { colors } = useAppTheme();
  const isAiSender = message.sender_id === AI_SENDER_ID;

  return (
    <View className={isMine ? 'items-end' : 'items-start'}>
      <View
        className={`max-w-[85%] rounded-2xl px-4 py-3 ${
          isMine
            ? 'rounded-br-md bg-academic-maroon dark:bg-[#C6635E]'
            : `rounded-bl-md bg-white dark:bg-[#1F1F26] ${isAiSender ? 'border border-academic-maroon/30 dark:border-[#C6635E]/30' : ''}`
        }`}
      >
        {isAiSender ? (
          <View className="mb-1.5 flex-row items-center gap-1.5">
            <Sparkles size={14} color={colors.accent} />
            <Text className="text-xs font-bold" style={{ color: colors.accent }}>
              CiteCircle AI
            </Text>
          </View>
        ) : null}

        <ScholarlyText
          text={message.content}
          style={{ fontSize: 14 }}
          color={isMine ? '#FBF9F4' : colors.text}
        />

        {message.attached_paper_id ? (
          <Pressable
            onPress={() => router.push(`/paper/${message.attached_paper_id}`)}
            className="mt-2 rounded-lg bg-black/10 px-3 py-2 dark:bg-white/10"
          >
            <Text className="text-xs font-semibold" style={{ color: isMine ? '#FBF9F4' : colors.accent }}>
              📄 View attached paper
            </Text>
          </Pressable>
        ) : null}
      </View>
      <Text className="mt-1 text-[11px] text-academic-muted dark:text-[#A6A6AC]">
        {formatRelativeTime(message.timestamp)}
      </Text>
    </View>
  );
}

function TypingIndicator() {
  const { colors } = useAppTheme();
  return (
    <View className="flex-row items-center gap-2">
      <View className="rounded-2xl rounded-bl-md border border-academic-maroon/20 bg-white px-4 py-3 dark:border-[#C6635E]/20 dark:bg-[#1F1F26]">
        <ActivityIndicator size="small" color={colors.accent} />
      </View>
      <Text className="text-xs font-medium" style={{ color: colors.accent }}>
        AI Copilot is thinking…
      </Text>
    </View>
  );
}
