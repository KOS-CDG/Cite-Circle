import { router } from 'expo-router';
import { ArrowUpRight, BookOpen, Compass, Flame, Search, Users, X } from 'lucide-react-native';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, FlatList, Pressable, RefreshControl, ScrollView, Text, TextInput, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/components/ui/theme-provider';
import { apiClient } from '@/lib/api-client';
import type { Circle, Paper } from '@/types';

const MOCK_CIRCLES: Circle[] = [
  {
    id: 'circle_ai',
    name: 'Generative AI & LLMs',
    description: 'Exploring neural architectures, reasoning, and alignment in modern deep learning models.',
    icon_emoji: '🤖',
    banner_color: 4281358937,
    member_count: 1420,
    category: 'Artificial Intelligence',
    post_count: 84,
    is_joined: true,
  },
  {
    id: 'circle_bio',
    name: 'Computational Biology',
    description: 'Protein structure prediction, genomic sequencing, and bio-ML applications.',
    icon_emoji: '🧬',
    banner_color: 4283141200,
    member_count: 890,
    category: 'Biology & Biotech',
    post_count: 45,
    is_joined: false,
  },
  {
    id: 'circle_quantum',
    name: 'Quantum Optics & Computing',
    description: 'Quantum error correction, superconducting qubits, and photonic quantum information.',
    icon_emoji: '⚛️',
    banner_color: 4280145200,
    member_count: 615,
    category: 'Physics',
    post_count: 32,
    is_joined: false,
  },
];

const MOCK_PAPERS: Paper[] = [
  {
    id: 'paper_deepseek',
    title: 'DeepSeek-V3 Technical Report: Architecture and Training Dynamics',
    abstract: 'We introduce DeepSeek-V3, a Multi-head Latent Attention (MLA) transformer trained on 14.8T tokens with Mixture-of-Experts.',
    year: 2024,
    journal: 'arXiv preprint',
    doi: '10.48550/arXiv.2412.19437',
    citation_count: 482,
    pdf_url: 'https://arxiv.org/pdf/2412.19437.pdf',
    circle_id: 'circle_ai',
    is_published: true,
    ai_score: 96,
  },
  {
    id: 'paper_alphafold',
    title: 'Accurate Structure Prediction of Biomolecular Complexes with AlphaFold 3',
    abstract: 'AlphaFold 3 expands protein structure prediction to complex biological interactions including proteins, nucleic acids, and small molecules.',
    year: 2024,
    journal: 'Nature',
    doi: '10.1038/s41586-024-07487-w',
    citation_count: 1250,
    pdf_url: null,
    circle_id: 'circle_bio',
    is_published: true,
    ai_score: 99,
  },
  {
    id: 'paper_flash',
    title: 'FlashAttention-2: Faster Attention with Better Parallelism and Work Partitioning',
    abstract: 'We present FlashAttention-2, optimizing work partitioning across GPU thread blocks to achieve 2x speedup over standard attention.',
    year: 2023,
    journal: 'NeurIPS',
    doi: '10.48550/arXiv.2307.08691',
    citation_count: 2140,
    pdf_url: null,
    circle_id: 'circle_ai',
    is_published: true,
    ai_score: 94,
  },
];

const FIELDS = ['All', 'AI & ML', 'CompBio', 'Quantum', 'Neuroscience', 'NLP'];

export default function ExploreScreen() {
  const { colors } = useAppTheme();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedField, setSelectedField] = useState('All');

  const [circles, setCircles] = useState<Circle[]>([]);
  const [papers, setPapers] = useState<Paper[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const loadData = useCallback(async () => {
    try {
      const fetchedCircles = await apiClient.get<Circle[]>('/circles?limit=10');
      const fetchedPapers = await apiClient.get<Paper[]>('/papers?limit=10');
      setCircles(fetchedCircles.length > 0 ? fetchedCircles : MOCK_CIRCLES);
      setPapers(fetchedPapers.length > 0 ? fetchedPapers : MOCK_PAPERS);
    } catch {
      setCircles(MOCK_CIRCLES);
      setPapers(MOCK_PAPERS);
    }
  }, []);

  useEffect(() => {
    setIsLoading(true);
    loadData().finally(() => setIsLoading(false));
  }, [loadData]);

  async function onRefresh() {
    setIsRefreshing(true);
    await loadData();
    setIsRefreshing(false);
  }

  const filteredPapers = useMemo(() => {
    const query = searchQuery.trim().toLowerCase();
    return papers.filter((paper) => {
      const matchesText = !query || paper.title.toLowerCase().includes(query) || paper.journal.toLowerCase().includes(query);
      const matchesField =
        selectedField === 'All' ||
        (selectedField === 'AI & ML' && (paper.title.includes('AI') || paper.title.includes('Deep') || paper.title.includes('Attention'))) ||
        (selectedField === 'CompBio' && (paper.title.includes('AlphaFold') || paper.title.includes('Bio'))) ||
        (selectedField === 'Quantum' && paper.title.includes('Quantum'));
      return matchesText && matchesField;
    });
  }, [papers, searchQuery, selectedField]);

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-academic-paper dark:bg-[#15151A]">
        <ActivityIndicator color={colors.accent} />
      </View>
    );
  }

  return (
    <SafeAreaView edges={['top']} className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <View className="px-4 pb-2 pt-2">
        <Text className="font-serif text-2xl font-bold text-academic-ink dark:text-[#EFEAE0]">Explore Research</Text>
        <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">Discover groundbreaking papers, topics & circles</Text>
      </View>

      {/* Search Input */}
      <View className="mx-4 my-2 flex-row items-center gap-2 rounded-2xl border border-academic-gold/20 bg-white px-3.5 py-2.5 shadow-sm dark:border-[#33333D] dark:bg-[#1F1F26]">
        <Search size={18} color={colors.textSecondary} />
        <TextInput
          value={searchQuery}
          onChangeText={setSearchQuery}
          placeholder="Search papers, authors, topics…"
          placeholderTextColor={colors.textSecondary}
          className="flex-1 text-sm text-academic-ink dark:text-[#EFEAE0]"
        />
        {searchQuery ? (
          <Pressable onPress={() => setSearchQuery('')} hitSlop={8}>
            <X size={16} color={colors.textSecondary} />
          </Pressable>
        ) : null}
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        refreshControl={<RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} tintColor={colors.accent} />}
        contentContainerStyle={{ paddingBottom: 32 }}
      >
        {/* Field Filters */}
        <ScrollView horizontal showsHorizontalScrollIndicator={false} className="px-4 py-2">
          <View className="flex-row gap-2 pr-6">
            {FIELDS.map((field) => {
              const active = selectedField === field;
              return (
                <Pressable
                  key={field}
                  onPress={() => setSelectedField(field)}
                  className={`rounded-full border px-3.5 py-1.5 ${
                    active
                      ? 'border-academic-maroon bg-academic-maroon dark:border-[#C6635E] dark:bg-[#C6635E]'
                      : 'border-academic-gold/25 bg-white dark:bg-[#1F1F26]'
                  }`}
                >
                  <Text className={`text-xs font-semibold ${active ? 'text-[#FBF9F4]' : 'text-academic-muted dark:text-[#A6A6AC]'}`}>
                    {field}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </ScrollView>

        {/* Featured Circles */}
        <View className="mt-4 px-4">
          <View className="flex-row items-center justify-between pb-2">
            <View className="flex-row items-center gap-1.5">
              <Users size={18} color={colors.accent} />
              <Text className="font-serif text-lg font-bold text-academic-navy dark:text-[#5D82AE]">Research Circles</Text>
            </View>
          </View>

          <ScrollView horizontal showsHorizontalScrollIndicator={false} className="-mx-4 px-4 py-1">
            <View className="flex-row gap-3 pr-6">
              {circles.map((circle) => (
                <Pressable
                  key={circle.id}
                  onPress={() => router.push(`/circle/${circle.id}`)}
                  className="w-64 rounded-2xl border border-academic-gold/20 bg-white p-4 shadow-sm dark:border-[#33333D] dark:bg-[#1F1F26]"
                >
                  <Text className="font-serif text-base font-bold text-academic-ink dark:text-[#EFEAE0]" numberOfLines={1}>
                    {circle.name}
                  </Text>
                  <Text className="mt-1 text-xs text-academic-muted dark:text-[#A6A6AC]" numberOfLines={2}>
                    {circle.description}
                  </Text>

                  <View className="mt-3 flex-row items-center justify-between border-t border-academic-gold/10 pt-2.5 dark:border-[#2A2A33]">
                    <Text className="text-xs font-semibold text-academic-navy dark:text-[#5D82AE]">
                      {circle.member_count} scholars
                    </Text>
                    <View className="flex-row items-center gap-1 rounded-full bg-academic-gold/15 px-2.5 py-1">
                      <Text className="text-xs font-bold text-academic-navy dark:text-[#5D82AE]">View</Text>
                      <ArrowUpRight size={12} color={colors.accentSecondary} />
                    </View>
                  </View>
                </Pressable>
              ))}
            </View>
          </ScrollView>
        </View>

        {/* Trending Papers Section */}
        <View className="mt-6 px-4">
          <View className="flex-row items-center gap-1.5 pb-3">
            <Flame size={18} color={colors.accent} />
            <Text className="font-serif text-lg font-bold text-academic-navy dark:text-[#5D82AE]">Trending Research Papers</Text>
          </View>

          <View className="gap-3">
            {filteredPapers.map((paper) => (
              <Pressable
                key={paper.id}
                onPress={() => router.push(`/paper/${paper.id}`)}
                className="rounded-2xl border border-academic-gold/20 bg-white p-4 shadow-sm dark:border-[#33333D] dark:bg-[#1F1F26]"
              >
                <View className="flex-row items-center justify-between">
                  <View className="flex-row items-center gap-1.5 rounded-full bg-academic-navy/10 px-2.5 py-0.5 dark:bg-[#5D82AE]/15">
                    <BookOpen size={12} color={colors.accentSecondary} />
                    <Text className="text-xs font-bold text-academic-navy dark:text-[#5D82AE]">{paper.journal}</Text>
                  </View>
                  <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">{paper.year}</Text>
                </View>

                <Text className="mt-2 text-base font-bold text-academic-ink dark:text-[#EFEAE0]">{paper.title}</Text>
                <Text className="mt-1 font-mono text-xs text-academic-muted dark:text-[#A6A6AC]" numberOfLines={1}>
                  DOI: {paper.doi}
                </Text>
                <Text className="mt-2 text-xs leading-relaxed text-academic-muted dark:text-[#A6A6AC]" numberOfLines={2}>
                  {paper.abstract}
                </Text>

                <View className="mt-3 flex-row items-center justify-between border-t border-academic-gold/10 pt-2 dark:border-[#2A2A33]">
                  <Text className="text-xs font-semibold text-academic-maroon dark:text-[#C6635E]">
                    {paper.citation_count} citations
                  </Text>
                  <View className="flex-row items-center gap-1">
                    <Text className="text-xs font-bold text-academic-navy dark:text-[#5D82AE]">Read Paper</Text>
                    <ArrowUpRight size={14} color={colors.accentSecondary} />
                  </View>
                </View>
              </Pressable>
            ))}
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

