import { Stack, useLocalSearchParams } from 'expo-router';
import { Check, Plus, Users } from 'lucide-react-native';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { PostCard } from '@/components/post-card';
import { PostComposerModal } from '@/components/post-composer-modal';
import { useAppTheme } from '@/components/ui/theme-provider';
import { router } from 'expo-router';
import { ApiError, apiClient } from '@/lib/api-client';
import { argbToCss } from '@/lib/format';
import type { Circle, CircleMembershipResult, EndorsePostResult, Post } from '@/types';

export default function CircleDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors } = useAppTheme();

  const [circle, setCircle] = useState<Circle | null>(null);
  const [posts, setPosts] = useState<Post[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isJoining, setIsJoining] = useState(false);
  const [showComposer, setShowComposer] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [circleData, postsData] = await Promise.all([
        apiClient.get<Circle>(`/circles/${id}`),
        apiClient.get<Post[]>(`/posts/circle/${id}`),
      ]);
      setCircle(circleData);
      setPosts(postsData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load this circle.');
    }
  }, [id]);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  async function toggleJoin() {
    if (!circle) return;
    setIsJoining(true);
    try {
      const path = circle.is_joined ? `/circles/${circle.id}/leave` : `/circles/${circle.id}/join`;
      const result = await apiClient.post<CircleMembershipResult>(path);
      setCircle({ ...circle, is_joined: result.is_joined, member_count: result.member_count });
    } catch {
      // Leave circle state as-is on failure.
    } finally {
      setIsJoining(false);
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
      setPosts((current) =>
        current.map((post) =>
          post.id === postId
            ? { ...post, is_endorsed: !post.is_endorsed, endorse_count: post.endorse_count + (post.is_endorsed ? -1 : 1) }
            : post,
        ),
      );
    }
  }

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  if (error || !circle) {
    return (
      <SafeAreaView className="flex-1 items-center justify-center gap-3 bg-academic-paper px-8 dark:bg-[#15151A]">
        <Text className="text-center text-base text-academic-muted dark:text-[#A6A6AC]">
          {error ?? 'Circle not found.'}
        </Text>
      </SafeAreaView>
    );
  }

  return (
    <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <Stack.Screen options={{ title: circle.name }} />
      <FlatList
        data={posts}
        keyExtractor={(post) => post.id}
        contentContainerClassName="gap-3 px-4 pb-6"
        renderItem={({ item }) => (
          <PostCard post={item} onPress={(postId) => router.push(`/post/${postId}`)} onEndorse={onEndorse} />
        )}
        ListHeaderComponent={
          <View className="mb-4">
            <View
              className="h-24 items-center justify-center rounded-2xl"
              style={{ backgroundColor: argbToCss(circle.banner_color) }}
            >
              <Text style={{ fontSize: 40 }}>{circle.icon_emoji}</Text>
            </View>

            <View className="mt-3 flex-row items-start justify-between">
              <View className="flex-1 pr-3">
                <Text className="font-serif text-xl font-bold text-academic-ink dark:text-[#EFEAE0]">
                  {circle.name}
                </Text>
                <View className="mt-1 flex-row items-center gap-1">
                  <Users size={13} color={colors.textSecondary} />
                  <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">
                    {circle.member_count} members • {circle.category}
                  </Text>
                </View>
              </View>

              <Pressable
                onPress={toggleJoin}
                disabled={isJoining}
                className={`flex-row items-center gap-1.5 rounded-full px-3.5 py-2 ${
                  circle.is_joined ? 'bg-academic-muted/20' : 'bg-academic-maroon dark:bg-[#C6635E]'
                }`}
              >
                {isJoining ? (
                  <ActivityIndicator size="small" color={circle.is_joined ? colors.textSecondary : '#FBF9F4'} />
                ) : (
                  <>
                    {circle.is_joined ? (
                      <Check size={14} color={colors.textSecondary} />
                    ) : (
                      <Plus size={14} color="#FBF9F4" />
                    )}
                    <Text
                      className="text-sm font-semibold"
                      style={{ color: circle.is_joined ? colors.textSecondary : '#FBF9F4' }}
                    >
                      {circle.is_joined ? 'Joined' : 'Join'}
                    </Text>
                  </>
                )}
              </Pressable>
            </View>

            {circle.description ? (
              <Text className="mt-3 text-sm text-academic-ink dark:text-[#EFEAE0]">{circle.description}</Text>
            ) : null}

            {circle.is_joined ? (
              <Pressable
                onPress={() => setShowComposer(true)}
                className="mt-4 items-center rounded-xl border border-dashed border-academic-gold/40 py-3"
              >
                <Text className="text-sm font-semibold text-academic-muted dark:text-[#A6A6AC]">
                  Post to c/{circle.name}
                </Text>
              </Pressable>
            ) : null}
          </View>
        }
        ListEmptyComponent={
          <View className="items-center gap-2 py-16">
            <Text className="text-4xl">📝</Text>
            <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">No posts yet</Text>
            <Text className="text-center text-sm text-academic-muted dark:text-[#A6A6AC]">
              Be the first to post a discussion topic in this circle!
            </Text>
          </View>
        }
      />

      <PostComposerModal
        visible={showComposer}
        onClose={() => setShowComposer(false)}
        onPosted={(post) => {
          setPosts((current) => [post, ...current]);
          setShowComposer(false);
        }}
        initialCircleId={circle.id}
        initialCircleName={circle.name}
      />
    </View>
  );
}
