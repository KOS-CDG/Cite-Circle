import { Image } from 'expo-image';
import { router, Stack } from 'expo-router';
import { Check } from 'lucide-react-native';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/components/ui/theme-provider';
import { apiClient } from '@/lib/api-client';
import type { ConnectionResult, User } from '@/types';

type Tab = 'connections' | 'requests' | 'discover';

const TABS: { key: Tab; label: string }[] = [
  { key: 'connections', label: 'Connections' },
  { key: 'requests', label: 'Requests' },
  { key: 'discover', label: 'Discover' },
];

export default function NetworkScreen() {
  const { colors } = useAppTheme();
  const [tab, setTab] = useState<Tab>('discover');
  const [discoverUsers, setDiscoverUsers] = useState<User[]>([]);
  const [pendingIds, setPendingIds] = useState<Set<string>>(new Set());
  const [isLoading, setIsLoading] = useState(true);

  const load = useCallback(async () => {
    const users = await apiClient.get<User[]>('/users/suggested');
    setDiscoverUsers(users);
  }, []);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  async function connect(userId: string) {
    setPendingIds((current) => new Set(current).add(userId));
    try {
      await apiClient.post<ConnectionResult>(`/users/${userId}/connect`);
    } catch {
      setPendingIds((current) => {
        const next = new Set(current);
        next.delete(userId);
        return next;
      });
    }
  }

  return (
    <SafeAreaView edges={['bottom']} className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <Stack.Screen options={{ title: 'Network' }} />

      <View className="flex-row border-b border-academic-gold/15 dark:border-[#33333D]">
        {TABS.map((t) => (
          <Pressable key={t.key} onPress={() => setTab(t.key)} className="flex-1 items-center py-3">
            <Text
              className="text-sm"
              style={{ color: tab === t.key ? colors.accent : colors.textSecondary, fontWeight: tab === t.key ? '700' : '500' }}
            >
              {t.label}
              {t.key === 'discover' ? '' : ` (0)`}
            </Text>
            {tab === t.key ? <View className="mt-2 h-0.5 w-8 rounded-full" style={{ backgroundColor: colors.accent }} /> : null}
          </Pressable>
        ))}
      </View>

      {isLoading ? (
        <View className="flex-1 items-center justify-center">
          <ActivityIndicator color={colors.accent} />
        </View>
      ) : tab === 'connections' ? (
        <EmptyState emoji="🤝" title="Grow your network" subtitle="You haven't connected with other researchers yet. Explore Discover to begin." />
      ) : tab === 'requests' ? (
        <EmptyState emoji="📬" title="No requests" subtitle="You have no incoming connection invites at the moment." />
      ) : (
        <FlatList
          data={discoverUsers}
          keyExtractor={(u) => u.id}
          numColumns={2}
          columnWrapperStyle={{ gap: 12, paddingHorizontal: 16 }}
          contentContainerStyle={{ gap: 12, paddingVertical: 16 }}
          renderItem={({ item }) => (
            <DiscoverCard user={item} isPending={pendingIds.has(item.id)} onConnect={() => connect(item.id)} />
          )}
          ListEmptyComponent={
            <EmptyState emoji="🔍" title="No suggestions yet" subtitle="Check back soon for researchers in your field." />
          }
        />
      )}
    </SafeAreaView>
  );
}

function EmptyState({ emoji, title, subtitle }: { emoji: string; title: string; subtitle: string }) {
  return (
    <View className="flex-1 items-center justify-center gap-2 px-8">
      <Text className="text-4xl">{emoji}</Text>
      <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">{title}</Text>
      <Text className="text-center text-sm text-academic-muted dark:text-[#A6A6AC]">{subtitle}</Text>
    </View>
  );
}

function DiscoverCard({ user, isPending, onConnect }: { user: User; isPending: boolean; onConnect: () => void }) {
  const { colors } = useAppTheme();

  return (
    <Pressable
      onPress={() => router.push(`/profile/${user.id}`)}
      className="flex-1 items-center rounded-2xl border border-academic-gold/15 bg-white p-4 dark:border-[#33333D] dark:bg-[#1F1F26]"
    >
      {user.avatar_url ? (
        <Image source={{ uri: user.avatar_url }} style={{ width: 56, height: 56, borderRadius: 28 }} />
      ) : (
        <View className="h-14 w-14 items-center justify-center rounded-full bg-academic-parchment dark:bg-[#2A2A33]">
          <Text className="text-xl font-bold text-academic-maroon dark:text-[#C6635E]">
            {user.name.charAt(0).toUpperCase()}
          </Text>
        </View>
      )}

      <Text numberOfLines={1} className="mt-3 text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">
        {user.name}
      </Text>
      <Text numberOfLines={1} className="text-xs text-academic-muted dark:text-[#A6A6AC]">
        {user.institution || 'Independent researcher'}
      </Text>

      <Pressable
        onPress={onConnect}
        disabled={isPending}
        className="mt-3 w-full flex-row items-center justify-center gap-1.5 rounded-lg py-2"
        style={{ backgroundColor: isPending ? colors.backgroundElement : colors.accent }}
      >
        {isPending ? (
          <>
            <Check size={14} color={colors.textSecondary} />
            <Text className="text-xs font-semibold" style={{ color: colors.textSecondary }}>
              Pending
            </Text>
          </>
        ) : (
          <Text className="text-xs font-semibold text-academic-paper">Connect</Text>
        )}
      </Pressable>
    </Pressable>
  );
}
