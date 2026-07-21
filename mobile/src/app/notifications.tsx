import { router, Stack } from 'expo-router';
import {
  Award,
  CheckCheck,
  MessageCircle,
  Quote,
  Sparkles,
  UserPlus,
  Users,
  type LucideIcon,
} from 'lucide-react-native';
import { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, RefreshControl, Text, View } from 'react-native';

import { useAppTheme } from '@/components/ui/theme-provider';
import { apiClient } from '@/lib/api-client';
import { formatRelativeTime } from '@/lib/format';
import type { Notification, NotificationType } from '@/types';

const ICON_BY_TYPE: Record<NotificationType, LucideIcon> = {
  ENDORSEMENT: Award,
  COMMENT: MessageCircle,
  CONNECTION: UserPlus,
  CIRCLE_INVITE: Users,
  AI_APPROVED: Sparkles,
  CITATION: Quote,
  NEW_FOLLOWER: UserPlus,
};

function navigateToTarget(notification: Notification) {
  switch (notification.type) {
    case 'ENDORSEMENT':
    case 'COMMENT':
      if (notification.target_id) router.push(`/post/${notification.target_id}`);
      return;
    case 'CITATION':
      if (notification.target_id) router.push(`/paper/${notification.target_id}`);
      return;
    case 'CIRCLE_INVITE':
      if (notification.target_id) router.push(`/circle/${notification.target_id}`);
      return;
    case 'CONNECTION':
    case 'NEW_FOLLOWER':
      router.push(`/profile/${notification.actor_id}`);
      return;
    default:
  }
}

export default function NotificationsScreen() {
  const { colors } = useAppTheme();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const load = useCallback(async () => {
    const data = await apiClient.get<Notification[]>('/notifications');
    setNotifications(data);
  }, []);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  async function onRefresh() {
    setIsRefreshing(true);
    await load().catch(() => {});
    setIsRefreshing(false);
  }

  async function markRead(notification: Notification) {
    if (notification.is_read) return;
    setNotifications((current) =>
      current.map((n) => (n.id === notification.id ? { ...n, is_read: true } : n)),
    );
    try {
      await apiClient.post(`/notifications/${notification.id}/read`);
    } catch {
      // Leave the optimistic read state — a stale unread badge is harmless.
    }
  }

  async function markAllRead() {
    const unread = notifications.filter((n) => !n.is_read);
    setNotifications((current) => current.map((n) => ({ ...n, is_read: true })));
    await Promise.all(unread.map((n) => apiClient.post(`/notifications/${n.id}/read`).catch(() => {})));
  }

  function onNotificationPress(notification: Notification) {
    markRead(notification);
    navigateToTarget(notification);
  }

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  const hasUnread = notifications.some((n) => !n.is_read);

  return (
    <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <Stack.Screen
        options={{
          title: 'Notifications',
          headerRight: () =>
            hasUnread ? (
              <Pressable onPress={markAllRead} hitSlop={8}>
                <CheckCheck size={20} color={colors.accent} />
              </Pressable>
            ) : null,
        }}
      />

      <FlatList
        data={notifications}
        keyExtractor={(n) => n.id}
        refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} tintColor={colors.accent} />}
        renderItem={({ item }) => <NotificationRow notification={item} onPress={() => onNotificationPress(item)} />}
        ListEmptyComponent={
          <View className="items-center gap-2 px-8 py-20">
            <Text className="text-4xl">🔔</Text>
            <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">All caught up!</Text>
            <Text className="text-center text-sm text-academic-muted dark:text-[#A6A6AC]">
              When researchers endorse your posts or cite your papers, you'll see it here.
            </Text>
          </View>
        }
      />
    </View>
  );
}

function NotificationRow({ notification, onPress }: { notification: Notification; onPress: () => void }) {
  const { colors } = useAppTheme();
  const Icon = ICON_BY_TYPE[notification.type] ?? Sparkles;

  return (
    <Pressable
      onPress={onPress}
      className="flex-row items-center gap-4 border-b border-academic-gold/10 px-4 py-3.5 dark:border-[#2A2A33]"
      style={{ backgroundColor: notification.is_read ? 'transparent' : `${colors.accent}0F` }}
    >
      <View className="h-9 w-9 items-center justify-center rounded-full" style={{ backgroundColor: `${colors.accent}26` }}>
        <Icon size={17} color={colors.accent} />
      </View>

      <View className="flex-1">
        <Text
          className="text-sm text-academic-ink dark:text-[#EFEAE0]"
          style={{ fontWeight: notification.is_read ? '400' : '700' }}
        >
          {notification.content}
        </Text>
        <Text className="mt-1 text-xs text-academic-muted dark:text-[#A6A6AC]">
          {formatRelativeTime(notification.timestamp)}
        </Text>
      </View>

      {!notification.is_read ? <View className="h-2 w-2 rounded-full bg-academic-maroon dark:bg-[#C6635E]" /> : null}
    </Pressable>
  );
}
