import { Image } from 'expo-image';
import { Stack, useLocalSearchParams } from 'expo-router';
import { Send } from 'lucide-react-native';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { PostCard } from '@/components/post-card';
import { ScholarlyText } from '@/components/ui/scholarly-text';
import { useAppTheme } from '@/components/ui/theme-provider';
import { formatRelativeTime, roleEmoji } from '@/lib/format';
import { ApiError, apiClient } from '@/lib/api-client';
import { useUserById } from '@/lib/user-cache';
import type { Comment, CommentCreate, EndorsePostResult, Post } from '@/types';

export default function PostDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { colors } = useAppTheme();

  const [post, setPost] = useState<Post | null>(null);
  const [comments, setComments] = useState<Comment[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [commentText, setCommentText] = useState('');
  const [replyingTo, setReplyingTo] = useState<Comment | null>(null);
  const [isPosting, setIsPosting] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [postData, commentsData] = await Promise.all([
        apiClient.get<Post>(`/posts/${id}`),
        apiClient.get<Comment[]>(`/posts/${id}/comments`),
      ]);
      setPost(postData);
      setComments(commentsData);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not load this post.');
    }
  }, [id]);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  const childrenByParent = useMemo(() => {
    const map = new Map<string | null, Comment[]>();
    for (const comment of comments) {
      const key = comment.parent_id;
      const bucket = map.get(key) ?? [];
      bucket.push(comment);
      map.set(key, bucket);
    }
    return map;
  }, [comments]);

  async function onEndorsePost() {
    if (!post) return;
    setPost({ ...post, is_endorsed: !post.is_endorsed, endorse_count: post.endorse_count + (post.is_endorsed ? -1 : 1) });
    try {
      const result = await apiClient.post<EndorsePostResult>(`/posts/${post.id}/endorse`);
      setPost((current) => (current ? { ...current, is_endorsed: result.endorsed, endorse_count: result.endorse_count } : current));
    } catch {
      setPost((current) =>
        current
          ? { ...current, is_endorsed: !current.is_endorsed, endorse_count: current.endorse_count + (current.is_endorsed ? -1 : 1) }
          : current,
      );
    }
  }

  async function submitComment() {
    if (!commentText.trim() || !post) return;
    setIsPosting(true);
    try {
      const body: CommentCreate = { content: commentText.trim(), parent_id: replyingTo?.id ?? null };
      const created = await apiClient.post<Comment>(`/posts/${post.id}/comments`, body);
      setComments((current) => [...current, created]);
      setPost((current) => (current ? { ...current, comment_count: current.comment_count + 1 } : current));
      setCommentText('');
      setReplyingTo(null);
    } catch {
      // Leave the draft in place so the user can retry.
    } finally {
      setIsPosting(false);
    }
  }

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  if (error || !post) {
    return (
      <SafeAreaView className="flex-1 items-center justify-center gap-3 bg-academic-paper px-8 dark:bg-[#15151A]">
        <Text className="text-center text-base text-academic-muted dark:text-[#A6A6AC]">
          {error ?? 'Post not found.'}
        </Text>
      </SafeAreaView>
    );
  }

  const rootComments = childrenByParent.get(null) ?? [];

  return (
    <KeyboardAvoidingView
      className="flex-1 bg-academic-paper dark:bg-[#15151A]"
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
    >
      <Stack.Screen options={{ title: 'Discussion' }} />

      <ScrollView contentContainerClassName="pb-4">
        <View className="px-4 pt-4">
          <PostCard post={post} onPress={() => {}} onEndorse={onEndorsePost} />
        </View>

        <View className="mt-5 border-t border-academic-gold/15 px-4 pt-4">
          <Text className="mb-2 text-base font-bold text-academic-ink dark:text-[#EFEAE0]">Comments</Text>

          {rootComments.length === 0 ? (
            <Text className="py-8 text-center text-sm text-academic-muted dark:text-[#A6A6AC]">
              No comments yet. Start the discussion!
            </Text>
          ) : (
            rootComments.map((comment) => (
              <CommentThread
                key={comment.id}
                comment={comment}
                depth={0}
                childrenByParent={childrenByParent}
                onReply={setReplyingTo}
              />
            ))
          )}
        </View>
      </ScrollView>

      <SafeAreaView edges={['bottom']} className="border-t border-academic-gold/15 bg-white dark:bg-[#1F1F26]">
        {replyingTo ? (
          <View className="flex-row items-center justify-between px-4 pt-2">
            <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">Replying to a comment</Text>
            <Pressable onPress={() => setReplyingTo(null)} hitSlop={8}>
              <Text className="text-xs font-bold text-red-500">Cancel</Text>
            </Pressable>
          </View>
        ) : null}
        <View className="flex-row items-center gap-2 p-3">
          <TextInput
            value={commentText}
            onChangeText={setCommentText}
            placeholder={replyingTo ? 'Write a reply…' : 'Write a comment…'}
            placeholderTextColor={colors.textSecondary}
            multiline
            className="max-h-24 flex-1 rounded-full border border-academic-gold/20 px-4 py-2.5 text-sm text-academic-ink dark:border-[#33333D] dark:text-[#EFEAE0]"
          />
          <Pressable
            onPress={submitComment}
            disabled={!commentText.trim() || isPosting}
            className="h-10 w-10 items-center justify-center rounded-full bg-academic-maroon dark:bg-[#C6635E]"
          >
            {isPosting ? <ActivityIndicator size="small" color="#FBF9F4" /> : <Send size={16} color="#FBF9F4" />}
          </Pressable>
        </View>
      </SafeAreaView>
    </KeyboardAvoidingView>
  );
}

function CommentThread({
  comment,
  depth,
  childrenByParent,
  onReply,
}: {
  comment: Comment;
  depth: number;
  childrenByParent: Map<string | null, Comment[]>;
  onReply: (comment: Comment) => void;
}) {
  const author = useUserById(comment.author_id);
  const replies = childrenByParent.get(comment.id) ?? [];

  return (
    <View style={{ paddingLeft: depth > 0 ? 16 : 0 }} className="mb-4">
      <View className="flex-row gap-3">
        {author?.avatar_url ? (
          <Image source={{ uri: author.avatar_url }} style={{ width: depth > 0 ? 28 : 36, height: depth > 0 ? 28 : 36, borderRadius: 18 }} />
        ) : (
          <View
            style={{ width: depth > 0 ? 28 : 36, height: depth > 0 ? 28 : 36 }}
            className="items-center justify-center rounded-full bg-academic-parchment dark:bg-[#2A2A33]"
          >
            <Text className="text-xs font-bold text-academic-maroon dark:text-[#C6635E]">
              {(author?.name ?? '?').charAt(0).toUpperCase()}
            </Text>
          </View>
        )}

        <View className="flex-1">
          <View className="flex-row items-center gap-1.5">
            <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">
              {author?.name ?? 'Loading…'}
            </Text>
            {author ? <Text className="text-[11px]">{roleEmoji(author.role)}</Text> : null}
          </View>

          <View className="mt-1">
            <ScholarlyText text={comment.content} style={{ fontSize: 14 }} />
          </View>

          <View className="mt-1.5 flex-row items-center gap-4">
            <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">
              {formatRelativeTime(comment.timestamp)}
            </Text>
            <Pressable onPress={() => onReply(comment)}>
              <Text className="text-xs font-bold text-academic-maroon dark:text-[#C6635E]">Reply</Text>
            </Pressable>
          </View>
        </View>
      </View>

      {replies.map((reply) => (
        <CommentThread key={reply.id} comment={reply} depth={depth + 1} childrenByParent={childrenByParent} onReply={onReply} />
      ))}
    </View>
  );
}
