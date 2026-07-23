import { Image } from 'expo-image';
import { router } from 'expo-router';
import { BadgeCheck, Bell, LogOut, RefreshCw, Users } from 'lucide-react-native';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Alert, Linking, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { PostCard } from '@/components/post-card';
import { Chip } from '@/components/ui/chip';
import { useAppTheme } from '@/components/ui/theme-provider';
import { apiClient, ApiError } from '@/lib/api-client';
import { useAuthStore } from '@/store/auth-store';
import type { EndorsePostResult, OrcidSyncResult, OrcidSyncState, Post, Publication, SavePostResult, User } from '@/types';


type Tab = 'posts' | 'publications';

const MOCK_SCHOLAR_USER: User = {
  id: 'me',
  name: 'Dr. Alexander Wright',
  avatar_url: '',
  role: 'RESEARCHER',
  institution: 'Stanford AI Institute',
  field_of_study: 'Artificial Intelligence & Multi-Agent Systems',
  bio: 'Focusing on LLM reasoning, neural-symbolic integration, and autonomous scientific discovery systems.',
  orcid_id: '0000-0003-4912-8800',
  follower_count: 1850,
  following_count: 340,
  citation_count: 4230,
  external_citation_count: 3800,
  publication_count: 18,
  orcid_verified: true,
  is_verified: true,
  interests: ['Multi-Agent AI', 'Neural Reasoning', 'Automated Science'],
};

export default function ProfileScreen() {
  const { colors } = useAppTheme();
  const authUser = useAuthStore((state) => state.user);
  const user = authUser || MOCK_SCHOLAR_USER;
  const setUser = useAuthStore((state) => state.setUser);
  const signOut = useAuthStore((state) => state.signOut);

  const [tab, setTab] = useState<Tab>('posts');
  const [posts, setPosts] = useState<Post[]>([]);
  const [publications, setPublications] = useState<Publication[]>([]);
  const [syncState, setSyncState] = useState<OrcidSyncState | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSyncing, setIsSyncing] = useState(false);

  const load = useCallback(async () => {
    try {
      const [feed, pubs, sync] = await Promise.all([
        apiClient.get<Post[]>('/posts?limit=100').catch(() => []),
        apiClient.get<Publication[]>('/users/me/publications').catch(() => []),
        apiClient.get<OrcidSyncState>('/users/me/orcid/state').catch(() => null),
      ]);
      setPosts(feed.filter((post) => post.author_id === user.id));
      setPublications(pubs);
      setSyncState(sync);
    } catch {
      setPosts([]);
      setPublications([]);
    }
  }, [user.id]);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  async function syncOrcid() {
    setIsSyncing(true);
    try {
      const result = await apiClient.post<OrcidSyncResult>('/users/me/orcid/sync');
      Alert.alert(
        result.status === 'SUCCESS' ? 'Sync complete' : 'Partial sync',
        result.message || `Synced ${result.works_synced} of ${result.total_available} works.`,
      );
      const [pubs, sync, freshUser] = await Promise.all([
        apiClient.get<Publication[]>('/users/me/publications').catch(() => []),
        apiClient.get<OrcidSyncState>('/users/me/orcid/state').catch(() => null),
        apiClient.get<User>('/users/me').catch(() => null),
      ]);
      setPublications(pubs);
      setSyncState(sync);
      if (freshUser) setUser(freshUser);
    } catch (err) {
      Alert.alert('Sync failed', err instanceof ApiError ? err.message : 'Please try again.');
    } finally {
      setIsSyncing(false);
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

  async function onSave(postId: string) {
    setPosts((current) =>
      current.map((post) => (post.id === postId ? { ...post, is_saved: !post.is_saved } : post)),
    );
    try {
      const result = await apiClient.post<SavePostResult>(`/posts/${postId}/save`);
      setPosts((current) =>
        current.map((post) => (post.id === postId ? { ...post, is_saved: result.saved } : post)),
      );
    } catch {
      setPosts((current) =>
        current.map((post) => (post.id === postId ? { ...post, is_saved: !post.is_saved } : post)),
      );
    }
  }

  function confirmSignOut() {
    Alert.alert('Sign out?', 'You can sign back in anytime.', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Sign Out', style: 'destructive', onPress: () => signOut() },
    ]);
  }

  if (!user || isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  return (
    <SafeAreaView edges={['top']} className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <View className="flex-row items-center justify-end gap-4 px-4 pt-2">
        <Pressable onPress={() => router.push('/network')} hitSlop={8}>
          <Users size={22} color={colors.text} />
        </Pressable>
        <Pressable onPress={() => router.push('/notifications')} hitSlop={8}>
          <Bell size={20} color={colors.text} />
        </Pressable>
        <Pressable onPress={confirmSignOut} hitSlop={8}>
          <LogOut size={20} color={colors.textSecondary} />
        </Pressable>
      </View>

      <ScrollView contentContainerClassName="pb-10">
        <View className="items-center px-6 pt-2">
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

          <View className="mt-5 w-full flex-row items-center justify-evenly rounded-xl bg-academic-parchment/60 py-3 dark:bg-[#1F1F26]">
            <StatItem count={user.follower_count} label="Followers" />
            <View className="h-6 w-px bg-academic-gold/20 dark:bg-[#33333D]" />
            <StatItem count={user.following_count} label="Following" />
            <View className="h-6 w-px bg-academic-gold/20 dark:bg-[#33333D]" />
            <StatItem count={user.citation_count} label="Citations" />
          </View>

          {user.bio ? <Text className="mt-5 text-sm text-academic-ink dark:text-[#EFEAE0]">{user.bio}</Text> : null}

          {user.interests.length > 0 ? (
            <View className="mt-4 w-full flex-row flex-wrap gap-2">
              {user.interests.map((interest) => (
                <Chip key={interest} label={interest} selected={false} onPress={() => {}} />
              ))}
            </View>
          ) : null}
        </View>

        <View className="mt-6 flex-row border-b border-academic-gold/15 dark:border-[#33333D]">
          <Pressable onPress={() => setTab('posts')} className="flex-1 items-center py-3">
            <Text className="text-sm" style={{ color: tab === 'posts' ? colors.accent : colors.textSecondary, fontWeight: tab === 'posts' ? '700' : '500' }}>
              Posts ({posts.length})
            </Text>
          </Pressable>
          <Pressable onPress={() => setTab('publications')} className="flex-1 items-center py-3">
            <Text className="text-sm" style={{ color: tab === 'publications' ? colors.accent : colors.textSecondary, fontWeight: tab === 'publications' ? '700' : '500' }}>
              Publications ({publications.length})
            </Text>
          </Pressable>
        </View>

        {tab === 'posts' ? (
          <View className="gap-3 px-4 pt-4">
            {posts.length === 0 ? (
              <EmptyState emoji="📄" title="No posts yet" subtitle="Your published posts will show up here." />
            ) : (
              posts.map((post) => (
                <PostCard
                  key={post.id}
                  post={post}
                  onPress={(id) => router.push(`/post/${id}`)}
                  onEndorse={onEndorse}
                  onSave={onSave}
                />
              ))
            )}
          </View>

        ) : (
          <View className="px-4 pt-4">
            <OrcidSyncHeader
              orcidId={user.orcid_id}
              syncState={syncState}
              isSyncing={isSyncing}
              publicationCount={publications.length}
              onSync={syncOrcid}
            />

            <View className="mt-4 gap-3">
              {publications.length === 0 ? (
                <EmptyState
                  emoji="🔬"
                  title={user.orcid_id ? 'No publications yet' : 'No ORCID linked'}
                  subtitle={
                    user.orcid_id
                      ? 'Tap Sync to pull your publications and citation counts from OpenAlex.'
                      : 'Add your ORCID iD to your profile, then sync to import your indexed work.'
                  }
                />
              ) : (
                publications.map((pub) => (
                  <Pressable
                    key={pub.id}
                    onPress={() => pub.open_access_url && Linking.openURL(pub.open_access_url)}
                    className="rounded-xl border border-academic-gold/15 bg-white p-4 dark:border-[#33333D] dark:bg-[#1F1F26]"
                  >
                    <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">{pub.title}</Text>
                    <Text className="mt-1 text-xs text-academic-muted dark:text-[#A6A6AC]">
                      {pub.journal || 'Unknown venue'} • {pub.publication_year ?? '—'} • {pub.citation_count} citations
                    </Text>
                  </Pressable>
                ))
              )}
            </View>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

function OrcidSyncHeader({
  orcidId,
  syncState,
  isSyncing,
  publicationCount,
  onSync,
}: {
  orcidId: string;
  syncState: OrcidSyncState | null;
  isSyncing: boolean;
  publicationCount: number;
  onSync: () => void;
}) {
  const { colors } = useAppTheme();

  return (
    <View className="rounded-xl border border-academic-gold/15 bg-white p-4 dark:border-[#33333D] dark:bg-[#1F1F26]">
      <View className="flex-row items-center justify-between">
        <View className="flex-1 pr-3">
          <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">OpenAlex / ORCID Sync</Text>
          <Text className="mt-0.5 text-xs text-academic-muted dark:text-[#A6A6AC]">
            {orcidId ? `${publicationCount} publications synced` : 'No ORCID iD on file'}
          </Text>
          {syncState?.last_error ? (
            <Text className="mt-1 text-xs text-red-500">{syncState.last_error}</Text>
          ) : null}
        </View>
        <Pressable
          onPress={onSync}
          disabled={isSyncing || !orcidId}
          className="flex-row items-center gap-1.5 rounded-lg bg-academic-maroon px-3 py-2 dark:bg-[#C6635E]"
          style={{ opacity: isSyncing || !orcidId ? 0.5 : 1 }}
        >
          {isSyncing ? (
            <ActivityIndicator size="small" color="#FBF9F4" />
          ) : (
            <RefreshCw size={14} color="#FBF9F4" />
          )}
          <Text className="text-xs font-semibold text-academic-paper">Sync</Text>
        </Pressable>
      </View>
    </View>
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

function EmptyState({ emoji, title, subtitle }: { emoji: string; title: string; subtitle: string }) {
  return (
    <View className="items-center gap-2 py-16">
      <Text className="text-4xl">{emoji}</Text>
      <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">{title}</Text>
      <Text className="text-center text-sm text-academic-muted dark:text-[#A6A6AC]">{subtitle}</Text>
    </View>
  );
}
