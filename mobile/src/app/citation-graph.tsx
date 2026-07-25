import { router, Stack, useLocalSearchParams } from 'expo-router';
import { Network, Share2, Sparkles, TrendingUp } from 'lucide-react-native';
import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { CitationGraphView } from '@/components/CitationGraphView';
import { CoAuthorNetworkView } from '@/components/CoAuthorNetworkView';
import { NodeDetailBottomSheet } from '@/components/NodeDetailBottomSheet';
import { ResearcherImpactCard } from '@/components/ResearcherImpactCard';
import { useAppTheme } from '@/components/ui/theme-provider';
import { apiClient } from '@/lib/api-client';
import type { CitationGraphNode, CitationGraphResponse, CoauthorGraphNode, CoauthorGraphResponse } from '@/types/graph';

type GraphMode = 'citations' | 'coauthors' | 'analytics';

export default function CitationGraphScreen() {
  const { paper_id, user_id, initialMode } = useLocalSearchParams<{
    paper_id?: string;
    user_id?: string;
    initialMode?: GraphMode;
  }>();
  const { colors } = useAppTheme();

  const [mode, setMode] = useState<GraphMode>(initialMode || (paper_id ? 'citations' : 'coauthors'));
  const [depth, setDepth] = useState<number>(2);
  const [minCitations, setMinCitations] = useState<number>(0);

  const [citationData, setCitationData] = useState<CitationGraphResponse | null>(null);
  const [coauthorData, setCoauthorData] = useState<CoauthorGraphResponse | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedPaperNode, setSelectedPaperNode] = useState<CitationGraphNode | null>(null);
  const [selectedAuthorNode, setSelectedAuthorNode] = useState<CoauthorGraphNode | null>(null);

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      if (mode === 'citations') {
        if (!paper_id) {
          setError('No research paper selected for citation analysis.');
          return;
        }
        const query = `?depth=${depth}&min_citations=${minCitations}`;
        const res = await apiClient.get<CitationGraphResponse>(`/papers/${paper_id}/citation-graph${query}`);
        setCitationData(res);
      } else {
        if (!user_id) {
          setError('No user selected for co-author network analysis.');
          return;
        }
        const res = await apiClient.get<CoauthorGraphResponse>(`/users/${user_id}/coauthor-graph`);
        setCoauthorData(res);
      }
    } catch (err: any) {
      setError(err?.message || 'Failed to load visualizer graph data.');
    } finally {
      setIsLoading(false);
    }
  }, [mode, depth, minCitations, paper_id, user_id]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  return (
    <SafeAreaView edges={['bottom']} className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <Stack.Screen
        options={{
          title: mode === 'citations' ? 'Citation Lineage' : mode === 'coauthors' ? 'Co-Author Network' : 'Impact Analytics',
          headerRight: () => (
            <Pressable onPress={() => {}} hitSlop={8} className="pr-1">
              <Share2 size={20} color={colors.text} />
            </Pressable>
          ),
        }}
      />

      {/* Segmented Mode Selector */}
      <View className="flex-row border-b border-academic-gold/15 bg-white px-4 py-2 dark:border-[#33333D] dark:bg-[#1F1F26]">
        <Pressable
          onPress={() => setMode('citations')}
          className={`flex-1 flex-row items-center justify-center gap-1.5 rounded-xl py-2 ${
            mode === 'citations' ? 'bg-academic-maroon dark:bg-[#C6635E]' : ''
          }`}
        >
          <Sparkles size={14} color={mode === 'citations' ? '#FFFFFF' : colors.textSecondary} />
          <Text
            className={`text-xs font-bold ${
              mode === 'citations' ? 'text-white' : 'text-academic-muted dark:text-[#A6A6AC]'
            }`}
          >
            Citations
          </Text>
        </Pressable>

        <Pressable
          onPress={() => setMode('coauthors')}
          className={`flex-1 flex-row items-center justify-center gap-1.5 rounded-xl py-2 ${
            mode === 'coauthors' ? 'bg-academic-maroon dark:bg-[#C6635E]' : ''
          }`}
        >
          <Network size={14} color={mode === 'coauthors' ? '#FFFFFF' : colors.textSecondary} />
          <Text
            className={`text-xs font-bold ${
              mode === 'coauthors' ? 'text-white' : 'text-academic-muted dark:text-[#A6A6AC]'
            }`}
          >
            Co-Authors
          </Text>
        </Pressable>

        <Pressable
          onPress={() => setMode('analytics')}
          className={`flex-1 flex-row items-center justify-center gap-1.5 rounded-xl py-2 ${
            mode === 'analytics' ? 'bg-academic-maroon dark:bg-[#C6635E]' : ''
          }`}
        >
          <TrendingUp size={14} color={mode === 'analytics' ? '#FFFFFF' : colors.textSecondary} />
          <Text
            className={`text-xs font-bold ${
              mode === 'analytics' ? 'text-white' : 'text-academic-muted dark:text-[#A6A6AC]'
            }`}
          >
            Analytics
          </Text>
        </Pressable>
      </View>

      {/* Main Content Area */}
      {isLoading ? (
        <View className="flex-1 items-center justify-center">
          <ActivityIndicator size="large" color={colors.accent} />
          <Text className="mt-3 text-xs text-academic-muted dark:text-[#A6A6AC]">Building force layout network…</Text>
        </View>
      ) : error ? (
        <View className="flex-1 items-center justify-center px-8">
          <Text className="text-center text-sm text-academic-muted dark:text-[#A6A6AC]">{error}</Text>
          <Pressable onPress={loadData} className="mt-3 rounded-xl bg-academic-maroon px-4 py-2">
            <Text className="text-xs font-bold text-white">Retry</Text>
          </Pressable>
        </View>
      ) : (
        <ScrollView contentContainerClassName="p-4 gap-4 pb-12">
          {mode === 'citations' && citationData ? (
            <View className="gap-3">
              {/* Summary Stats Header */}
              <View className="flex-row items-center justify-between rounded-xl border border-academic-gold/15 bg-white p-3 dark:border-[#33333D] dark:bg-[#1F1F26]">
                <View className="flex-row items-center gap-2">
                  <Text className="text-xs font-bold text-academic-ink dark:text-[#EFEAE0]">
                    {citationData.summary.total_papers} Papers
                  </Text>
                  <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">•</Text>
                  <Text className="text-xs font-bold text-academic-ink dark:text-[#EFEAE0]">
                    {citationData.summary.total_citations} Citations
                  </Text>
                </View>
                <View className="rounded-full bg-academic-gold/20 px-2.5 py-0.5">
                  <Text className="text-[10px] font-bold text-academic-navy dark:text-[#5D82AE]">
                    {depth}-Hop Lineage
                  </Text>
                </View>
              </View>

              {/* Interactive Canvas */}
              <CitationGraphView
                nodes={citationData.nodes}
                edges={citationData.edges}
                selectedNodeId={selectedPaperNode?.id || null}
                onSelectNode={(node) => setSelectedPaperNode(node)}
                depth={depth}
                onDepthChange={setDepth}
              />
            </View>
          ) : mode === 'coauthors' && coauthorData ? (
            <View className="gap-3">
              <CoAuthorNetworkView
                nodes={coauthorData.nodes}
                edges={coauthorData.edges}
                clusters={coauthorData.clusters}
                selectedNodeId={selectedAuthorNode?.id || null}
                onSelectNode={(node) => setSelectedAuthorNode(node)}
              />
            </View>
          ) : mode === 'analytics' && coauthorData ? (
            <ResearcherImpactCard analytics={coauthorData.analytics} />
          ) : null}
        </ScrollView>
      )}

      {/* Node Inspection Bottom Sheet */}
      <NodeDetailBottomSheet
        visible={Boolean(selectedPaperNode || selectedAuthorNode)}
        paperNode={selectedPaperNode}
        authorNode={selectedAuthorNode}
        onClose={() => {
          setSelectedPaperNode(null);
          setSelectedAuthorNode(null);
        }}
        depth={depth}
        onDepthChange={setDepth}
        minCitations={minCitations}
        onMinCitationsChange={setMinCitations}
      />
    </SafeAreaView>
  );
}
