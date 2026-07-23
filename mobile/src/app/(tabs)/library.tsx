import { router } from 'expo-router';
import { Folder, FolderX, Plus, Search, X } from 'lucide-react-native';
import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { ActivityIndicator, Modal, Pressable, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { PostCard } from '@/components/post-card';
import { Button } from '@/components/ui/button';
import { useAppTheme } from '@/components/ui/theme-provider';
import { ApiError, apiClient } from '@/lib/api-client';
import type { EndorsePostResult, Paper, Post, SavePostResult, Shelf, ShelfCreate } from '@/types';


const MOCK_LIBRARY_SHELVES: Shelf[] = [
  {
    id: 'shelf_1',
    owner_id: 'me',
    name: 'LLM Reasoning & Compute',
    description: 'Papers on test-time scaling, chain-of-thought, and self-consistency.',
    paper_ids: ['paper_deepseek', 'paper_flash'],
  },
  {
    id: 'shelf_2',
    owner_id: 'me',
    name: 'Protein Dynamics & ML',
    description: 'AlphaFold 3, ESMFold, and molecular dynamics applications.',
    paper_ids: ['paper_alphafold'],
  },
];

const MOCK_LIBRARY_PAPERS: Paper[] = [
  {
    id: 'paper_deepseek',
    title: 'DeepSeek-V3 Technical Report: Architecture and Training Dynamics',
    abstract: 'We introduce DeepSeek-V3, a Multi-head Latent Attention (MLA) transformer.',
    year: 2024,
    journal: 'arXiv preprint',
    doi: '10.48550/arXiv.2412.19437',
    citation_count: 482,
    pdf_url: null,
    circle_id: null,
    is_published: true,
    ai_score: 95,
  },
  {
    id: 'paper_alphafold',
    title: 'Accurate Structure Prediction of Biomolecular Complexes with AlphaFold 3',
    abstract: 'AlphaFold 3 expands protein structure prediction to complex biological interactions.',
    year: 2024,
    journal: 'Nature',
    doi: '10.1038/s41586-024-07487-w',
    citation_count: 1250,
    pdf_url: null,
    circle_id: null,
    is_published: true,
    ai_score: 98,
  },
];

export default function LibraryScreen() {
  const { colors } = useAppTheme();

  const [shelves, setShelves] = useState<Shelf[]>([]);
  const [papers, setPapers] = useState<Paper[]>([]);
  const [savedPosts, setSavedPosts] = useState<Post[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedTab, setSelectedTab] = useState<'papers' | 'posts' | 'shelves'>('papers');
  const [searchQuery, setSearchQuery] = useState('');
  const [activeShelfId, setActiveShelfId] = useState<string | null>(null);
  const [showCreateShelf, setShowCreateShelf] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [shelvesData, postsData] = await Promise.all([
        apiClient.get<Shelf[]>('/shelves').catch(() => []),
        apiClient.get<Post[]>('/posts/saved').catch(() => []),
      ]);
      const savedIds = new Set(shelvesData.flatMap((shelf) => shelf.paper_ids));
      let papersData: Paper[] = [];
      if (savedIds.size > 0) {
        const allPapers = await apiClient.get<Paper[]>('/papers?limit=200').catch(() => []);
        papersData = allPapers.filter((paper) => savedIds.has(paper.id));
      }
      setShelves(shelvesData);
      setPapers(papersData);
      setSavedPosts(postsData);
    } catch {
      setShelves([]);
      setPapers([]);
      setSavedPosts([]);
    }
  }, []);

  useEffect(() => {
    setIsLoading(true);
    load().finally(() => setIsLoading(false));
  }, [load]);

  const activeShelf = shelves.find((shelf) => shelf.id === activeShelfId) ?? null;

  const filteredPapers = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return papers;
    return papers.filter((paper) => paper.title.toLowerCase().includes(query) || paper.journal.toLowerCase().includes(query));
  }, [papers, searchQuery]);

  const filteredPosts = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return savedPosts;
    return savedPosts.filter((post) => post.content.toLowerCase().includes(query));
  }, [savedPosts, searchQuery]);

  const filteredShelves = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    if (!query) return shelves;
    return shelves.filter(
      (shelf) => shelf.name.toLowerCase().includes(query) || shelf.description.toLowerCase().includes(query),
    );
  }, [shelves, searchQuery]);

  const shelfPapers = useMemo(() => {
    if (!activeShelf) return [];
    return papers.filter((paper) => activeShelf.paper_ids.includes(paper.id));
  }, [activeShelf, papers]);

  async function onEndorsePost(postId: string) {
    setSavedPosts((current) =>
      current.map((post) =>
        post.id === postId
          ? { ...post, is_endorsed: !post.is_endorsed, endorse_count: post.endorse_count + (post.is_endorsed ? -1 : 1) }
          : post,
      ),
    );
    try {
      const result = await apiClient.post<EndorsePostResult>(`/posts/${postId}/endorse`);
      setSavedPosts((current) =>
        current.map((post) =>
          post.id === postId ? { ...post, is_endorsed: result.endorsed, endorse_count: result.endorse_count } : post,
        ),
      );
    } catch {
      setSavedPosts((current) =>
        current.map((post) =>
          post.id === postId
            ? { ...post, is_endorsed: !post.is_endorsed, endorse_count: post.endorse_count + (post.is_endorsed ? -1 : 1) }
            : post,
        ),
      );
    }
  }

  async function onSavePost(postId: string) {
    setSavedPosts((current) => current.filter((post) => post.id !== postId));
    try {
      await apiClient.post<SavePostResult>(`/posts/${postId}/save`);
    } catch {
      await load();
    }
  }

  async function createShelf(name: string, description: string) {
    try {
      const body: ShelfCreate = { name, description };
      const created = await apiClient.post<Shelf>('/shelves', body);
      setShelves((current) => [...current, created]);
    } catch {
      // Leave the library state as-is on failure.
    }
  }

  async function removeFromShelf(paperId: string, shelfId: string) {
    setShelves((current) =>
      current.map((shelf) =>
        shelf.id === shelfId ? { ...shelf, paper_ids: shelf.paper_ids.filter((id) => id !== paperId) } : shelf,
      ),
    );
    try {
      await apiClient.delete(`/shelves/${shelfId}/papers/${paperId}`);
    } catch {
      await load();
    }
  }

  async function unsavePaper(paperId: string) {

    const containing = shelves.filter((shelf) => shelf.paper_ids.includes(paperId));
    setShelves((current) =>
      current.map((shelf) =>
        containing.some((c) => c.id === shelf.id)
          ? { ...shelf, paper_ids: shelf.paper_ids.filter((id) => id !== paperId) }
          : shelf,
      ),
    );
    setPapers((current) => current.filter((paper) => paper.id !== paperId));
    try {
      await Promise.all(containing.map((shelf) => apiClient.delete(`/shelves/${shelf.id}/papers/${paperId}`)));
    } catch {
      await load();
    }
  }

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  if (error) {
    return (
      <SafeAreaView className="flex-1 items-center justify-center gap-3 bg-academic-paper px-8 dark:bg-[#15151A]">
        <Text className="text-center text-base text-academic-muted dark:text-[#A6A6AC]">{error}</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView className="flex-1 bg-academic-paper dark:bg-[#15151A]" edges={['top']}>
      <View className="flex-row items-center justify-between px-4 pb-2 pt-2">
        <Text className="font-serif text-2xl font-bold text-academic-ink dark:text-[#EFEAE0]">
          {activeShelf ? activeShelf.name : 'Personal Library'}
        </Text>
        {activeShelf ? (
          <Pressable onPress={() => setActiveShelfId(null)} hitSlop={8}>
            <X size={20} color={colors.text} />
          </Pressable>
        ) : selectedTab === 'shelves' ? (
          <Pressable onPress={() => setShowCreateShelf(true)} hitSlop={8}>
            <Plus size={22} color={colors.text} />
          </Pressable>
        ) : null}
      </View>

      {!activeShelf ? (
        <>
          <View className="flex-row items-center gap-2 rounded-xl border border-academic-gold/20 bg-white px-3 py-2 mx-4 mb-2 dark:border-[#33333D] dark:bg-[#1F1F26]">
            <Search size={16} color={colors.textSecondary} />
            <TextInput
              value={searchQuery}
              onChangeText={setSearchQuery}
              placeholder="Search title, journal…"
              placeholderTextColor={colors.textSecondary}
              className="flex-1 text-sm text-academic-ink dark:text-[#EFEAE0]"
            />
            {searchQuery ? (
              <Pressable onPress={() => setSearchQuery('')} hitSlop={8}>
                <X size={16} color={colors.textSecondary} />
              </Pressable>
            ) : null}
          </View>

          <View className="mx-4 mb-2 flex-row rounded-xl bg-academic-parchment p-1 dark:bg-[#1F1F26]">
            <Pressable
              onPress={() => setSelectedTab('papers')}
              className={`flex-1 items-center rounded-lg py-2 ${selectedTab === 'papers' ? 'bg-white dark:bg-[#2A2A33]' : ''}`}
            >
              <Text
                className="text-xs font-semibold"
                style={{ color: selectedTab === 'papers' ? colors.text : colors.textSecondary }}
              >
                Papers ({papers.length})
              </Text>
            </Pressable>
            <Pressable
              onPress={() => setSelectedTab('posts')}
              className={`flex-1 items-center rounded-lg py-2 ${selectedTab === 'posts' ? 'bg-white dark:bg-[#2A2A33]' : ''}`}
            >
              <Text
                className="text-xs font-semibold"
                style={{ color: selectedTab === 'posts' ? colors.text : colors.textSecondary }}
              >
                Posts ({savedPosts.length})
              </Text>
            </Pressable>
            <Pressable
              onPress={() => setSelectedTab('shelves')}
              className={`flex-1 items-center rounded-lg py-2 ${selectedTab === 'shelves' ? 'bg-white dark:bg-[#2A2A33]' : ''}`}
            >
              <Text
                className="text-xs font-semibold"
                style={{ color: selectedTab === 'shelves' ? colors.text : colors.textSecondary }}
              >
                Shelves ({shelves.length})
              </Text>
            </Pressable>
          </View>
        </>
      ) : null}

      {activeShelf ? (
        <ScrollView contentContainerClassName="gap-3 p-4">
          {activeShelf.description ? (
            <Text className="mb-1 text-sm text-academic-muted dark:text-[#A6A6AC]">{activeShelf.description}</Text>
          ) : null}
          {shelfPapers.length === 0 ? (
            <EmptyState emoji="📄" title="Empty shelf" subtitle="Save papers and add them here from the paper detail screen." />
          ) : (
            shelfPapers.map((paper) => (
              <LibraryPaperCard
                key={paper.id}
                paper={paper}
                onPress={() => router.push(`/paper/${paper.id}`)}
                onRemove={() => removeFromShelf(paper.id, activeShelf.id)}
                removeIcon={<FolderX size={18} color={colors.textSecondary} />}
              />
            ))
          )}
        </ScrollView>
      ) : selectedTab === 'papers' ? (
        <ScrollView contentContainerClassName="gap-3 p-4">
          {filteredPapers.length === 0 ? (
            <EmptyState
              emoji="🔖"
              title={searchQuery ? 'No match found' : 'No saved papers'}
              subtitle={searchQuery ? 'Try a different search term.' : 'Save papers by tapping the bookmark icon on a paper.'}
            />
          ) : (
            filteredPapers.map((paper) => (
              <LibraryPaperCard
                key={paper.id}
                paper={paper}
                onPress={() => router.push(`/paper/${paper.id}`)}
                onRemove={() => unsavePaper(paper.id)}
              />
            ))
          )}
        </ScrollView>
      ) : selectedTab === 'posts' ? (
        <ScrollView contentContainerClassName="gap-3 p-4">
          {filteredPosts.length === 0 ? (
            <EmptyState
              emoji="🔖"
              title={searchQuery ? 'No match found' : 'No saved posts'}
              subtitle={searchQuery ? 'Try a different search term.' : 'Save posts by tapping the bookmark icon on post cards.'}
            />
          ) : (
            filteredPosts.map((post) => (
              <PostCard
                key={post.id}
                post={{ ...post, is_saved: true }}
                onPress={(id) => router.push(`/post/${id}`)}
                onEndorse={onEndorsePost}
                onSave={onSavePost}
              />
            ))
          )}
        </ScrollView>
      ) : (

        <ScrollView contentContainerClassName="gap-3 p-4">
          {filteredShelves.length === 0 ? (
            <EmptyState
              emoji="📁"
              title={searchQuery ? 'No match found' : 'No shelves yet'}
              subtitle={searchQuery ? 'Try a different search term.' : 'Create a shelf to group related research.'}
            />
          ) : (
            filteredShelves.map((shelf) => (
              <Pressable
                key={shelf.id}
                onPress={() => setActiveShelfId(shelf.id)}
                className="flex-row items-center gap-3 rounded-2xl border border-academic-gold/15 bg-white p-4 dark:border-[#33333D] dark:bg-[#1F1F26]"
              >
                <View className="h-11 w-11 items-center justify-center rounded-xl bg-academic-navy dark:bg-[#5D82AE]">
                  <Folder size={20} color="#FBF9F4" />
                </View>
                <View className="flex-1">
                  <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">{shelf.name}</Text>
                  {shelf.description ? (
                    <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]" numberOfLines={1}>
                      {shelf.description}
                    </Text>
                  ) : null}
                </View>
                <View className="rounded-full bg-academic-gold/20 px-2.5 py-1">
                  <Text className="text-xs font-bold text-academic-navy dark:text-[#5D82AE]">
                    {shelf.paper_ids.length}
                  </Text>
                </View>
              </Pressable>
            ))
          )}
        </ScrollView>
      )}

      <CreateShelfModal visible={showCreateShelf} onClose={() => setShowCreateShelf(false)} onCreate={createShelf} />
    </SafeAreaView>
  );
}

function LibraryPaperCard({
  paper,
  onPress,
  onRemove,
  removeIcon,
}: {
  paper: Paper;
  onPress: () => void;
  onRemove: () => void;
  removeIcon?: ReactNode;
}) {
  const { colors } = useAppTheme();
  return (
    <Pressable
      onPress={onPress}
      className="flex-row items-start justify-between gap-2 rounded-2xl border border-academic-gold/15 bg-white p-4 dark:border-[#33333D] dark:bg-[#1F1F26]"
    >
      <View className="flex-1">
        <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]" numberOfLines={2}>
          {paper.title}
        </Text>
        <Text className="mt-1 text-xs text-academic-muted dark:text-[#A6A6AC]">
          {[paper.journal, String(paper.year)].filter(Boolean).join(' • ')}
        </Text>
        <Text className="mt-1 text-xs text-academic-muted dark:text-[#A6A6AC]">
          {paper.citation_count} citation{paper.citation_count === 1 ? '' : 's'}
        </Text>
      </View>
      <Pressable onPress={onRemove} hitSlop={8}>
        {removeIcon ?? <X size={18} color={colors.textSecondary} />}
      </Pressable>
    </Pressable>
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

function CreateShelfModal({
  visible,
  onClose,
  onCreate,
}: {
  visible: boolean;
  onClose: () => void;
  onCreate: (name: string, description: string) => void;
}) {
  const { colors } = useAppTheme();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  function submit() {
    if (!name.trim()) return;
    onCreate(name.trim(), description.trim());
    setName('');
    setDescription('');
    onClose();
  }

  return (
    <Modal visible={visible} animationType="slide" presentationStyle="pageSheet" onRequestClose={onClose}>
      <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
        <SafeAreaView className="flex-1">
          <View className="flex-row items-center justify-between border-b border-academic-gold/15 px-4 py-3 dark:border-[#33333D]">
            <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">New Research Shelf</Text>
            <Pressable onPress={onClose} hitSlop={8}>
              <X size={22} color={colors.text} />
            </Pressable>
          </View>

          <View className="gap-3 p-4">
            <TextInput
              value={name}
              onChangeText={setName}
              placeholder="Shelf name (e.g. HCI & AI)"
              placeholderTextColor={colors.textSecondary}
              autoFocus
              className="rounded-xl border border-academic-gold/20 bg-white px-3 py-2.5 text-sm text-academic-ink dark:border-[#33333D] dark:bg-[#1F1F26] dark:text-[#EFEAE0]"
            />
            <TextInput
              value={description}
              onChangeText={setDescription}
              placeholder="Description (optional)"
              placeholderTextColor={colors.textSecondary}
              multiline
              className="min-h-[70px] rounded-xl border border-academic-gold/20 bg-white px-3 py-2.5 text-sm text-academic-ink dark:border-[#33333D] dark:bg-[#1F1F26] dark:text-[#EFEAE0]"
              style={{ textAlignVertical: 'top' }}
            />
            <Button label="Create Shelf" disabled={!name.trim()} onPress={submit} />
          </View>
        </SafeAreaView>
      </View>
    </Modal>
  );
}
