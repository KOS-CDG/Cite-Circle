import { Image } from 'expo-image';
import { router } from 'expo-router';
import { Sparkles } from 'lucide-react-native';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, RefreshControl, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/components/ui/theme-provider';
import { apiClient } from '@/lib/api-client';
import { formatRelativeTime } from '@/lib/format';
import { useAuthStore } from '@/store/auth-store';
import { useUserById } from '@/lib/user-cache';
import type { Conversation, Message, User } from '@/types';

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

interface ConversationRow {
  id: string;
  isAi: boolean;
  peerId: string | null;
  lastMessage: Message | null;
  unreadCount: number;
}

export default function MessagesScreen() {
  const { colors } = useAppTheme();
  const currentUser = useAuthStore((state) => state.user);

  const [rows, setRows] = useState<ConversationRow[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const load = useCallback(async () => {
    const conversations = await apiClient.get<Conversation[]>('/conversations');
    const withoutAi = conversations.filter((c) => c.id !== AI_CONVERSATION_ID);

    const resolved = await Promise.all(
      withoutAi.map(async (conv): Promise<ConversationRow> => {
        try {
          const messages = await apiClient.get<Message[]>(`/conversations/${conv.id}/messages`);
          const peer = messages.find((m) => m.sender_id !== currentUser?.id)?.sender_id ?? null;
          return {
            id: conv.id,
            isAi: false,
            peerId: peer,
            lastMessage: messages.at(-1) ?? null,
            unreadCount: messages.filter((m) => !m.is_read && m.sender_id !== currentUser?.id).length,
          };
        } catch {
          return { id: conv.id, isAi: false, peerId: null, lastMessage: null, unreadCount: 0 };
        }
      }),
    );

    let aiRow: ConversationRow;
    try {
      const aiMessages = await apiClient.get<Message[]>(`/conversations/${AI_CONVERSATION_ID}/messages`);
      aiRow = {
        id: AI_CONVERSATION_ID,
        isAi: true,
        peerId: AI_SENDER_ID,
        lastMessage: aiMessages.at(-1) ?? {
          id: 'msg_ai_1',
          conversation_id: AI_CONVERSATION_ID,
          sender_id: AI_SENDER_ID,
          content: 'Hello! I am your CiteCircle AI Copilot. Ask me anything about literature, methodology, or drafting your paper.',
          timestamp: Date.now(),
          is_read: true,
          attached_paper_id: null,
        },
        unreadCount: 0,
      };
    } catch {
      aiRow = {
        id: AI_CONVERSATION_ID,
        isAi: true,
        peerId: AI_SENDER_ID,
        lastMessage: {
          id: 'msg_ai_1',
          conversation_id: AI_CONVERSATION_ID,
          sender_id: AI_SENDER_ID,
          content: 'Hello! I am your CiteCircle AI Copilot. Ask me anything about literature, methodology, or drafting your paper.',
          timestamp: Date.now(),
          is_read: true,
          attached_paper_id: null,
        },
        unreadCount: 0,
      };
    }

    setRows([aiRow, ...resolved]);
  }, [currentUser?.id]);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  async function onRefresh() {
    setIsRefreshing(true);
    await load().catch(() => {});
    setIsRefreshing(false);
  }

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  return (
    <SafeAreaView edges={['top']} className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <View className="border-b border-academic-gold/15 px-4 py-3 dark:border-[#33333D]">
        <Text className="font-serif text-2xl font-bold text-academic-ink dark:text-[#EFEAE0]">Messages</Text>
      </View>

      <FlatList
        data={rows}
        keyExtractor={(row) => row.id}
        refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} tintColor={colors.accent} />}
        renderItem={({ item }) => <ConversationRowItem row={item} />}
        ListEmptyComponent={
          <View className="items-center gap-2 px-8 py-20">
            <Text className="text-4xl">✉️</Text>
            <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">No messages</Text>
            <Text className="text-center text-sm text-academic-muted dark:text-[#A6A6AC]">
              Chat with the CiteCircle AI Copilot about literature, methodology, or your drafts.
            </Text>
          </View>
        }
      />
    </SafeAreaView>
  );
}

function ConversationRowItem({ row }: { row: ConversationRow }) {
  const { colors } = useAppTheme();
  const resolvedPeer = useUserById(row.isAi ? null : row.peerId);
  const peer = row.isAi ? AI_COPILOT_USER : resolvedPeer;
  const unread = row.unreadCount > 0;

  if (!row.isAi && !row.peerId) return null;

  return (
    <Pressable
      onPress={() => router.push(`/chat/${row.id}`)}
      className="flex-row items-center gap-4 border-b border-academic-gold/10 px-4 py-3.5 dark:border-[#2A2A33]"
    >
      {row.isAi ? (
        <View className="h-12 w-12 items-center justify-center rounded-full bg-academic-maroon/15 dark:bg-[#C6635E]/20">
          <Sparkles size={20} color={colors.accent} />
        </View>
      ) : peer?.avatar_url ? (
        <Image source={{ uri: peer.avatar_url }} style={{ width: 48, height: 48, borderRadius: 24 }} />
      ) : (
        <View className="h-12 w-12 items-center justify-center rounded-full bg-academic-parchment dark:bg-[#2A2A33]">
          <Text className="text-lg font-bold text-academic-maroon dark:text-[#C6635E]">
            {(peer?.name ?? '?').charAt(0).toUpperCase()}
          </Text>
        </View>
      )}

      <View className="flex-1">
        <View className="flex-row items-center justify-between">
          <Text
            className="text-sm text-academic-ink dark:text-[#EFEAE0]"
            style={{ fontWeight: unread ? '700' : '600' }}
          >
            {peer?.name ?? 'Loading…'}
          </Text>
          {row.lastMessage ? (
            <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">
              {formatRelativeTime(row.lastMessage.timestamp)}
            </Text>
          ) : null}
        </View>
        <View className="mt-0.5 flex-row items-center justify-between">
          <Text
            numberOfLines={1}
            className="flex-1 text-xs text-academic-muted dark:text-[#A6A6AC]"
            style={unread ? { color: colors.text, fontWeight: '600' } : undefined}
          >
            {row.lastMessage?.content ?? 'No messages in this chat'}
          </Text>
          {unread ? <View className="ml-2 h-2.5 w-2.5 rounded-full bg-academic-maroon dark:bg-[#C6635E]" /> : null}
        </View>
      </View>
    </Pressable>
  );
}
