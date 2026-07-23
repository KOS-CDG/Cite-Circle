import { Image } from 'expo-image';
import { Stack, useLocalSearchParams } from 'expo-router';
import { BadgeCheck, ExternalLink as ExternalLinkIcon, UserCheck, UserPlus } from 'lucide-react-native';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Linking, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Chip } from '@/components/ui/chip';
import { useAppTheme } from '@/components/ui/theme-provider';
import { apiClient, ApiError } from '@/lib/api-client';
import { roleEmoji } from '@/lib/format';
import type { FollowResult, Publication, User } from '@/types';

export default function OtherProfileScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors } = useAppTheme();

  const [user, setUser] = useState<User | null>(null);
  const [publications, setPublications] = useState<Publication[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // The backend never exposes "am I already following this user" (see types/user.ts),
  // so we can only assume not-following until the toggle endpoint's response says otherwise.
  const [isFollowing, setIsFollowing] = useState(false);
  const [isFollowBusy, setIsFollowBusy] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [userData, pubs] = await Promise.all([
        apiClient.get<User>(`/users/${id}`),
        apiClient.get<Publication[]>(`/users/${id}/publications`).catch(() => []),
      ]);
      setUser(userData);
      setPublications(pubs);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load this profile.');
    }
  }, [id]);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  async function toggleFollow() {
    if (!user) return;
    setIsFollowBusy(true);
    try {
      const result = await apiClient.post<FollowResult>(`/users/${user.id}/follow`);
      setIsFollowing(result.following);
      setUser((current) =>
        current
          ? { ...current, follower_count: current.follower_count + (result.following ? 1 : -1) }
          : current,
      );
    } catch {
      // Leave follow state as-is on failure.
    } finally {
      setIsFollowBusy(false);
    }
  }

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  if (error || !user) {
    return (
      <SafeAreaView className="flex-1 items-center justify-center gap-3 bg-academic-paper px-8 dark:bg-[#15151A]">
        <Text className="text-center text-base text-academic-muted dark:text-[#A6A6AC]">
          {error ?? 'User not found.'}
        </Text>
      </SafeAreaView>
    );
  }

  return (
    <ScrollView className="flex-1 bg-academic-paper dark:bg-[#15151A]" contentContainerClassName="pb-10">
      <Stack.Screen options={{ title: user.name }} />

      <View className="items-center px-6 pt-8">
        {user.avatar_url ? (
          <Image source={{ uri: user.avatar_url }} style={{ width: 88, height: 88, borderRadius: 44 }} />
        ) : (
          <View className="h-[88px] w-[88px] items-center justify-center rounded-full bg-academic-parchment dark:bg-[#2A2A33]">
            <Text className="text-3xl font-bold text-academic-maroon dark:text-[#C6635E]">
              {user.name.charAt(0).toUpperCase()}
            </Text>
          </View>
        )}

        <View className="mt-3 flex-row items-center gap-1.5">
          <Text className="font-serif text-xl font-bold text-academic-ink dark:text-[#EFEAE0]">{user.name}</Text>
          <Text className="text-base">{roleEmoji(user.role)}</Text>
          {user.is_verified ? <BadgeCheck size={16} color={colors.accent} /> : null}
        </View>
        <Text className="mt-0.5 text-sm text-academic-muted dark:text-[#A6A6AC]">
          {user.institution || 'Independent researcher'}
        </Text>

        {user.orcid_id ? (
          <Text className="mt-1 font-mono text-xs font-bold text-academic-navy dark:text-[#5D82AE]">
            ORCID: {user.orcid_id}
          </Text>
        ) : null}

        <Pressable
          onPress={toggleFollow}
          disabled={isFollowBusy}
          className={`mt-4 flex-row items-center gap-2 rounded-full px-6 py-2.5 ${
            isFollowing ? 'bg-academic-muted/20' : 'bg-academic-maroon dark:bg-[#C6635E]'
          }`}
          style={{ opacity: isFollowBusy ? 0.6 : 1 }}
        >
          {isFollowBusy ? (
            <ActivityIndicator size="small" color={isFollowing ? colors.textSecondary : '#FBF9F4'} />
          ) : isFollowing ? (
            <UserCheck size={16} color={colors.textSecondary} />
          ) : (
            <UserPlus size={16} color="#FBF9F4" />
          )}
          <Text className="text-sm font-semibold" style={{ color: isFollowing ? colors.textSecondary : '#FBF9F4' }}>
            {isFollowing ? 'Following' : 'Follow'}
          </Text>
        </Pressable>

        <View className="mt-6 w-full flex-row items-center justify-evenly rounded-xl bg-academic-parchment/60 py-3 dark:bg-[#1F1F26]">
          <StatItem count={user.follower_count} label="Followers" />
          <View className="h-6 w-px bg-academic-gold/20 dark:bg-[#33333D]" />
          <StatItem count={user.following_count} label="Following" />
          <View className="h-6 w-px bg-academic-gold/20 dark:bg-[#33333D]" />
          <StatItem count={user.citation_count} label="Citations" />
        </View>

        <Pressable
          onPress={() => router.push({ pathname: '/citation-graph', params: { user_id: user.id, initialMode: 'coauthors' } })}
          className="mt-4 w-full flex-row items-center justify-center gap-2 rounded-xl border border-academic-gold/30 bg-academic-gold/10 py-3 dark:border-academic-gold/20 dark:bg-[#252530]"
        >
          <UserCheck size={16} color={colors.accent} />
          <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">Co-Author Network & Impact</Text>
        </Pressable>


        {user.bio ? (
          <Text className="mt-5 text-sm text-academic-ink dark:text-[#EFEAE0]">{user.bio}</Text>
        ) : null}

        {user.interests.length > 0 ? (
          <View className="mt-4 w-full flex-row flex-wrap gap-2">
            {user.interests.map((interest) => (
              <Chip key={interest} label={interest} selected={false} onPress={() => {}} />
            ))}
          </View>
        ) : null}
      </View>

      {publications.length > 0 ? (
        <View className="mt-8 px-4">
          <Text className="mb-3 text-base font-bold text-academic-ink dark:text-[#EFEAE0]">Publications</Text>
          {publications.map((pub) => (
            <Pressable
              key={pub.id}
              onPress={() => pub.open_access_url && Linking.openURL(pub.open_access_url)}
              className="mb-3 rounded-xl border border-academic-gold/15 bg-white p-4 dark:border-[#33333D] dark:bg-[#1F1F26]"
            >
              <View className="flex-row items-start justify-between gap-2">
                <Text className="flex-1 text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">{pub.title}</Text>
                {pub.open_access_url ? <ExternalLinkIcon size={14} color={colors.textSecondary} /> : null}
              </View>
              <Text className="mt-1 text-xs text-academic-muted dark:text-[#A6A6AC]">
                {pub.journal || 'Unknown venue'} • {pub.publication_year ?? '—'} • {pub.citation_count} citations
              </Text>
            </Pressable>
          ))}
        </View>
      ) : null}
    </ScrollView>
  );
}

function StatItem({ count, label }: { count: number; label: string }) {
  return (
    <View className="items-center">
      <Text className="text-lg font-bold text-academic-ink dark:text-[#EFEAE0]">{count}</Text>
      <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">{label}</Text>
    </View>
  );
}
