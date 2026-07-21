import { Image } from 'expo-image';
import { ArrowBigUp, MessageCircle } from 'lucide-react-native';
import { Pressable, Text, View } from 'react-native';

import { ScholarlyText } from '@/components/ui/scholarly-text';
import { useAppTheme } from '@/components/ui/theme-provider';
import { formatRelativeTime, roleEmoji } from '@/lib/format';
import { useUserById } from '@/lib/user-cache';
import type { Post, PostFlair } from '@/types';

const flairLabels: Record<PostFlair, string> = {
  QUESTION: 'Question',
  DISCUSSION: 'Discussion',
  PAPER_FEEDBACK: 'Paper Feedback',
  RESOURCE: 'Resource',
  NONE: '',
};

interface PostCardProps {
  post: Post;
  onPress: (postId: string) => void;
  onEndorse: (postId: string) => void;
}

export function PostCard({ post, onPress, onEndorse }: PostCardProps) {
  const { colors } = useAppTheme();
  const author = useUserById(post.author_id);

  return (
    <Pressable
      onPress={() => onPress(post.id)}
      className="rounded-2xl border border-academic-gold/15 bg-white p-4 dark:border-[#33333D] dark:bg-[#1F1F26]"
    >
      <View className="flex-row items-center gap-3">
        {author?.avatar_url ? (
          <Image source={{ uri: author.avatar_url }} style={{ width: 40, height: 40, borderRadius: 20 }} />
        ) : (
          <View className="h-10 w-10 items-center justify-center rounded-full bg-academic-parchment dark:bg-[#2A2A33]">
            <Text className="text-base font-bold text-academic-maroon dark:text-[#C6635E]">
              {(author?.name ?? '?').charAt(0).toUpperCase()}
            </Text>
          </View>
        )}
        <View className="flex-1">
          <View className="flex-row items-center gap-1.5">
            <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">
              {author?.name ?? 'Loading…'}
            </Text>
            {author ? <Text className="text-xs">{roleEmoji(author.role)}</Text> : null}
          </View>
          <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]" numberOfLines={1}>
            {author?.institution ? `${author.institution} • ` : ''}
            {formatRelativeTime(post.timestamp)}
          </Text>
        </View>
      </View>

      {post.flair !== 'NONE' ? (
        <View className="mt-3 self-start rounded bg-academic-gold/20 px-2 py-0.5">
          <Text className="text-[11px] font-bold text-academic-navy dark:text-[#5D82AE]">
            {flairLabels[post.flair]}
          </Text>
        </View>
      ) : null}

      <View className="mt-3">
        <ScholarlyText text={post.content} style={{ fontSize: 14 }} color={colors.text} />
      </View>

      <View className="mt-4 flex-row items-center gap-5">
        <Pressable
          onPress={() => onEndorse(post.id)}
          className="flex-row items-center gap-1.5 rounded-full px-2 py-1"
          hitSlop={8}
        >
          <ArrowBigUp
            size={18}
            color={post.is_endorsed ? colors.accent : colors.textSecondary}
            fill={post.is_endorsed ? colors.accent : 'transparent'}
          />
          <Text className="text-sm font-semibold" style={{ color: post.is_endorsed ? colors.accent : colors.textSecondary }}>
            {post.endorse_count}
          </Text>
        </Pressable>

        <View className="flex-row items-center gap-1.5">
          <MessageCircle size={17} color={colors.textSecondary} />
          <Text className="text-sm" style={{ color: colors.textSecondary }}>
            {post.comment_count}
          </Text>
        </View>
      </View>
    </Pressable>
  );
}
