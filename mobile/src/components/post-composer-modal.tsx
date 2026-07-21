import { X } from 'lucide-react-native';
import { useEffect, useState } from 'react';
import { ActivityIndicator, Modal, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/components/ui/theme-provider';
import { Button } from '@/components/ui/button';
import { Chip } from '@/components/ui/chip';
import { ApiError, apiClient } from '@/lib/api-client';
import type { Circle, Paper, Post, PostCreate, PostFlair } from '@/types';

const flairOptions: { value: PostFlair; label: string }[] = [
  { value: 'DISCUSSION', label: 'Discussion' },
  { value: 'QUESTION', label: 'Question' },
  { value: 'PAPER_FEEDBACK', label: 'Feedback' },
  { value: 'RESOURCE', label: 'Resource' },
];

interface PostComposerModalProps {
  visible: boolean;
  onClose: () => void;
  onPosted: (post: Post) => void;
  /** When set, the post is pinned to this circle and the circle picker is hidden. */
  initialCircleId?: string;
  initialCircleName?: string;
}

export function PostComposerModal({
  visible,
  onClose,
  onPosted,
  initialCircleId,
  initialCircleName,
}: PostComposerModalProps) {
  const { colors } = useAppTheme();

  const [content, setContent] = useState('');
  const [flair, setFlair] = useState<PostFlair>('DISCUSSION');
  const [circles, setCircles] = useState<Circle[]>([]);
  const [selectedCircleId, setSelectedCircleId] = useState<string | null>(null);

  const [paperIdInput, setPaperIdInput] = useState('');
  const [attachedPaper, setAttachedPaper] = useState<Paper | null>(null);
  const [isLookingUpPaper, setIsLookingUpPaper] = useState(false);
  const [paperError, setPaperError] = useState<string | null>(null);

  const [isPosting, setIsPosting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!visible || initialCircleId) return;
    apiClient
      .get<Circle[]>('/circles')
      .then(setCircles)
      .catch(() => setCircles([]));
  }, [visible, initialCircleId]);

  function reset() {
    setContent('');
    setFlair('DISCUSSION');
    setSelectedCircleId(null);
    setPaperIdInput('');
    setAttachedPaper(null);
    setPaperError(null);
    setFormError(null);
  }

  async function lookupPaper() {
    const id = paperIdInput.trim();
    if (!id) return;
    setPaperError(null);
    setIsLookingUpPaper(true);
    try {
      const paper = await apiClient.get<Paper>(`/papers/${id}`);
      setAttachedPaper(paper);
    } catch (err) {
      setAttachedPaper(null);
      setPaperError(err instanceof ApiError && err.status === 404 ? 'No paper found with that ID.' : 'Could not look up that paper.');
    } finally {
      setIsLookingUpPaper(false);
    }
  }

  async function submit() {
    if (!content.trim()) return;
    setFormError(null);
    setIsPosting(true);
    try {
      const body: PostCreate = {
        content: content.trim(),
        type: attachedPaper ? 'PAPER_SHARE' : 'DISCUSSION',
        flair,
        circle_id: initialCircleId ?? selectedCircleId,
        attached_paper_id: attachedPaper?.id ?? null,
      };
      const post = await apiClient.post<Post>('/posts', body);
      reset();
      onPosted(post);
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : 'Could not publish your post.');
    } finally {
      setIsPosting(false);
    }
  }

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
        <SafeAreaView className="flex-1">
          <View className="flex-row items-center justify-between border-b border-academic-gold/15 px-4 py-3">
            <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">
              {initialCircleName ? `Post in c/${initialCircleName}` : 'New Post'}
            </Text>
            <Pressable onPress={onClose} hitSlop={8}>
              <X size={22} color={colors.text} />
            </Pressable>
          </View>

          <ScrollView contentContainerClassName="gap-4 p-4" keyboardShouldPersistTaps="handled">
            <View className="flex-row flex-wrap gap-2">
              {flairOptions.map((option) => (
                <Chip
                  key={option.value}
                  label={option.label}
                  selected={flair === option.value}
                  onPress={() => setFlair(option.value)}
                />
              ))}
            </View>

            <TextInput
              value={content}
              onChangeText={setContent}
              placeholder="Share your findings, a question, or start a discussion…"
              placeholderTextColor={colors.textSecondary}
              multiline
              numberOfLines={8}
              className="min-h-[140px] rounded-2xl border border-academic-gold/20 bg-white p-3 text-base text-academic-ink dark:border-[#33333D] dark:bg-[#1F1F26] dark:text-[#EFEAE0]"
              style={{ textAlignVertical: 'top' }}
            />

            {!initialCircleId && circles.length > 0 ? (
              <View className="gap-2">
                <Text className="text-sm font-semibold text-academic-muted dark:text-[#A6A6AC]">
                  Tag a circle (optional)
                </Text>
                <View className="flex-row flex-wrap gap-2">
                  {circles.map((circle) => (
                    <Chip
                      key={circle.id}
                      label={`${circle.icon_emoji} ${circle.name}`}
                      selected={selectedCircleId === circle.id}
                      onPress={() => setSelectedCircleId((current) => (current === circle.id ? null : circle.id))}
                    />
                  ))}
                </View>
              </View>
            ) : null}

            <View className="gap-2">
              <Text className="text-sm font-semibold text-academic-muted dark:text-[#A6A6AC]">
                Attach a paper (optional)
              </Text>
              {attachedPaper ? (
                <View className="flex-row items-center justify-between rounded-xl border border-academic-gold/20 bg-academic-parchment p-3 dark:border-[#33333D] dark:bg-[#1F1F26]">
                  <View className="flex-1 pr-2">
                    <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]" numberOfLines={2}>
                      {attachedPaper.title}
                    </Text>
                    <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">
                      {attachedPaper.citation_count} citations
                    </Text>
                  </View>
                  <Pressable onPress={() => setAttachedPaper(null)} hitSlop={8}>
                    <X size={18} color={colors.textSecondary} />
                  </Pressable>
                </View>
              ) : (
                <View className="flex-row items-center gap-2">
                  <TextInput
                    value={paperIdInput}
                    onChangeText={(value) => {
                      setPaperIdInput(value);
                      setPaperError(null);
                    }}
                    placeholder="Paper ID"
                    placeholderTextColor={colors.textSecondary}
                    autoCapitalize="none"
                    className="flex-1 rounded-xl border border-academic-gold/20 bg-white px-3 py-2.5 text-sm text-academic-ink dark:border-[#33333D] dark:bg-[#1F1F26] dark:text-[#EFEAE0]"
                  />
                  <Pressable
                    onPress={lookupPaper}
                    disabled={!paperIdInput.trim() || isLookingUpPaper}
                    className="rounded-xl bg-academic-navy px-3 py-2.5 dark:bg-[#5D82AE]"
                  >
                    {isLookingUpPaper ? (
                      <ActivityIndicator size="small" color="#FBF9F4" />
                    ) : (
                      <Text className="text-sm font-semibold text-[#FBF9F4]">Attach</Text>
                    )}
                  </Pressable>
                </View>
              )}
              {paperError ? <Text className="text-xs text-red-500">{paperError}</Text> : null}
            </View>

            {formError ? <Text className="text-sm text-red-500">{formError}</Text> : null}

            <Button label="Publish Post" isLoading={isPosting} disabled={!content.trim()} onPress={submit} />
          </ScrollView>
        </SafeAreaView>
      </View>
    </Modal>
  );
}
