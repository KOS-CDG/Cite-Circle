import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, RefreshControl, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { router } from 'expo-router';
import { Plus } from 'lucide-react-native';

import { PostCard } from '@/components/post-card';
import { PostComposerModal } from '@/components/post-composer-modal';
import { useAppTheme } from '@/components/ui/theme-provider';
import { ApiError, apiClient } from '@/lib/api-client';
import type { EndorsePostResult, Post, PostFlair, PostType } from '@/types';

const PAGE_SIZE = 20;

type Category = 'All' | 'Papers' | 'Discussions' | 'Reviews';

const categories: Category[] = ['All', 'Papers', 'Discussions', 'Reviews'];

function matchesCategory(post: Post, category: Category): boolean {
  if (category === 'All') return true;
  if (category === 'Papers') return post.type === ('PAPER_SHARE' satisfies PostType);
  if (category === 'Discussions') return post.type === ('DISCUSSION' satisfies PostType);
  return post.flair === ('PAPER_FEEDBACK' satisfies PostFlair);
}

const MOCK_FEED_POSTS: Post[] = [
  {
    id: 'post_1',
    author_id: 'user_1',
    content:
      'Our latest paper explores dynamic test-time compute allocation. By allowing LLMs to execute step-by-step verification trees during inference, accuracy on MATH-500 improves by +18.4%. What are your thoughts on compute scaling vs architecture search?',
    type: 'PAPER_SHARE',
    timestamp: Date.now() - 7200000,
    endorse_count: 128,
    comment_count: 34,
    circle_id: null,
    attached_paper_id: 'paper_deepseek',
    milestone_text: null,
    flair: 'RESOURCE',
    image_url: null,
    is_endorsed: true,
    is_saved: false,
  },
  {
    id: 'post_2',
    author_id: 'user_2',
    content:
      'While AlphaFold 3 achieves remarkable accuracy on ligand binding complexes, flexible loop regions and dynamic conformational shifts still pose challenges. Here is our analysis comparing predicted vs experimental B-factors.',
    type: 'DISCUSSION',
    timestamp: Date.now() - 18000000,
    endorse_count: 245,
    comment_count: 52,
    circle_id: null,
    attached_paper_id: null,
    milestone_text: null,
    flair: 'DISCUSSION',
    image_url: null,
    is_endorsed: false,
    is_saved: false,
  },
];

export default function FeedScreen() {
  const { colors } = useAppTheme();

  const [posts, setPosts] = useState<Post[]>([]);
  const [category, setCategory] = useState<Category>('All');
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isLoadingMore, setIsLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showComposer, setShowComposer] = useState(false);

  const loadPage = useCallback(async (offset: number) => {
    const page = await apiClient.get<Post[]>(`/posts?limit=${PAGE_SIZE}&offset=${offset}`);
    return page;
  }, []);

  const refresh = useCallback(async () => {
    setError(null);
    try {
      const page = await loadPage(0);
      setPosts(page.length > 0 ? page : MOCK_FEED_POSTS);
      setHasMore(page.length === PAGE_SIZE);
    } catch {
      setPosts(MOCK_FEED_POSTS);
      setHasMore(false);
    }
  }, [loadPage]);

  useEffect(() => {
    setIsLoading(true);
    refresh().finally(() => setIsLoading(false));
  }, [refresh]);

  async function onRefresh() {
    setIsRefreshing(true);
    await refresh();
    setIsRefreshing(false);
  }

  async function onEndReached() {
    if (isLoadingMore || !hasMore || isLoading) return;
    setIsLoadingMore(true);
    try {
      const page = await loadPage(posts.length);
      setPosts((current) => [...current, ...page]);
      setHasMore(page.length === PAGE_SIZE);
    } catch {
      // Silent — the user can pull-to-refresh or scroll again to retry.
    } finally {
      setIsLoadingMore(false);
    }
  }

  async function onEndorse(postId: string) {
    setPosts((current) =>
      current.map((post) =>
        post.id === postId
          ? { ...post, is_endorsed: !post.is_endorsed, endorse_count: post.endorse_count + (post.is_endorsed ? -1 : 1) }
          : post,
      ),
    );
    try {
      const result = await apiClient.post<EndorsePostResult>(`/posts/${postId}/endorse`);
      setPosts((current) =>
        current.map((post) =>
          post.id === postId ? { ...post, is_endorsed: result.endorsed, endorse_count: result.endorse_count } : post,
        ),
      );
    } catch {
      // Revert the optimistic update on failure.
      setPosts((current) =>
        current.map((post) =>
          post.id === postId
            ? { ...post, is_endorsed: !post.is_endorsed, endorse_count: post.endorse_count + (post.is_endorsed ? -1 : 1) }
            : post,
        ),
      );
    }
  }

  const visiblePosts = posts.filter((post) => matchesCategory(post, category));

  return (
    <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <SafeAreaView className="flex-1" edges={['top']}>
        <View className="flex-row items-center justify-between px-4 pb-2 pt-2">
          <Text className="font-serif text-2xl font-bold text-academic-navy dark:text-[#5D82AE]">Cite-Circle</Text>
          <Pressable
            onPress={() => setShowComposer(true)}
            className="flex-row items-center gap-1 rounded-full bg-academic-maroon px-3 py-2 dark:bg-[#C6635E]"
          >
            <Plus size={16} color="#FBF9F4" />
            <Text className="text-sm font-semibold text-[#FBF9F4]">Post</Text>
          </Pressable>
        </View>

        <View className="flex-row gap-2 px-4 pb-3">
          {categories.map((item) => {
            const selected = item === category;
            return (
              <Pressable
                key={item}
                onPress={() => setCategory(item)}
                className={`rounded-full border px-3 py-1.5 ${
                  selected
                    ? 'border-academic-maroon bg-academic-maroon dark:border-[#C6635E] dark:bg-[#C6635E]'
                    : 'border-academic-gold/25 bg-transparent'
                }`}
              >
                <Text
                  className={`text-xs font-semibold ${
                    selected ? 'text-[#FBF9F4]' : 'text-academic-muted dark:text-[#A6A6AC]'
                  }`}
                >
                  {item}
                </Text>
              </Pressable>
            );
          })}
        </View>

        {isLoading ? (
          <View className="flex-1 items-center justify-center">
            <ActivityIndicator color={colors.accent} />
          </View>
        ) : error ? (
          <View className="flex-1 items-center justify-center gap-3 px-8">
            <Text className="text-center text-base text-academic-muted dark:text-[#A6A6AC]">{error}</Text>
            <Pressable onPress={onRefresh} className="rounded-full bg-academic-maroon px-4 py-2 dark:bg-[#C6635E]">
              <Text className="text-sm font-semibold text-[#FBF9F4]">Retry</Text>
            </Pressable>
          </View>
        ) : (
          <FlatList
            data={visiblePosts}
            keyExtractor={(post) => post.id}
            contentContainerClassName="gap-3 px-4 pb-6"
            renderItem={({ item }) => (
              <PostCard post={item} onPress={(postId) => router.push(`/post/${postId}`)} onEndorse={onEndorse} />
            )}
            refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} tintColor={colors.accent} />}
            onEndReachedThreshold={0.4}
            onEndReached={onEndReached}
            ListEmptyComponent={
              <View className="items-center gap-2 py-16">
                <Text className="text-4xl">📭</Text>
                <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">No posts found</Text>
                <Text className="text-center text-sm text-academic-muted dark:text-[#A6A6AC]">
                  Try another category, or be the first to post.
                </Text>
              </View>
            }
            ListFooterComponent={isLoadingMore ? <ActivityIndicator className="py-4" color={colors.accent} /> : null}
          />
        )}
      </SafeAreaView>

      <PostComposerModal
        visible={showComposer}
        onClose={() => setShowComposer(false)}
        onPosted={(post) => {
          setPosts((current) => [post, ...current]);
          setShowComposer(false);
        }}
      />
    </View>
  );
}
