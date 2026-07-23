import { router } from 'expo-router';
import { BookOpen, Calendar, ExternalLink, Filter, Layers, Sliders, User, X } from 'lucide-react-native';
import React, { useState } from 'react';
import { Modal, Pressable, ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/components/ui/theme-provider';
import type { CitationGraphNode, CoauthorGraphNode } from '@/types/graph';

interface NodeDetailBottomSheetProps {
  visible: boolean;
  paperNode: CitationGraphNode | null;
  authorNode: CoauthorGraphNode | null;
  onClose: () => void;
  depth?: number;
  onDepthChange?: (d: number) => void;
  minCitations?: number;
  onMinCitationsChange?: (c: number) => void;
}

export function NodeDetailBottomSheet({
  visible,
  paperNode,
  authorNode,
  onClose,
  depth = 2,
  onDepthChange,
  minCitations = 0,
  onMinCitationsChange,
}: NodeDetailBottomSheetProps) {
  const { colors } = useAppTheme();
  const [showFilters, setShowFilters] = useState(false);

  if (!paperNode && !authorNode) return null;

  const isPaper = Boolean(paperNode);

  return (
    <Modal visible={visible} animationType="slide" transparent presentationStyle="overCurrentContext" onRequestClose={onClose}>
      <Pressable onPress={onClose} className="flex-1 bg-black/40" />

      <View className="max-h-[85%] rounded-t-3xl border-t border-academic-gold/20 bg-academic-paper p-5 shadow-2xl dark:border-[#33333D] dark:bg-[#191920]">
        <SafeAreaView edges={['bottom']}>
          {/* Header Bar */}
          <View className="flex-row items-center justify-between border-b border-academic-gold/15 pb-3 dark:border-[#33333D]">
            <View className="flex-row items-center gap-2">
              <View className="h-2 w-10 rounded-full bg-academic-gold/40" />
              <Text className="text-xs font-bold uppercase text-academic-muted dark:text-[#A6A6AC]">
                {isPaper ? 'Paper Inspection' : 'Author Profile Preview'}
              </Text>
            </View>
            <View className="flex-row items-center gap-3">
              <Pressable onPress={() => setShowFilters(!showFilters)} className="rounded-lg bg-academic-gold/15 p-1.5 dark:bg-[#252530]">
                <Filter size={18} color={colors.accent} />
              </Pressable>
              <Pressable onPress={onClose} className="p-1">
                <X size={20} color={colors.text} />
              </Pressable>
            </View>
          </View>

          <ScrollView contentContainerClassName="gap-4 pt-4 pb-4">
            {/* Filter Controls Toggle Drawer */}
            {showFilters ? (
              <View className="gap-3 rounded-2xl border border-academic-gold/20 bg-white p-4 dark:border-[#33333D] dark:bg-[#1F1F26]">
                <View className="flex-row items-center gap-2 border-b border-academic-gold/15 pb-2 dark:border-[#33333D]">
                  <Sliders size={16} color={colors.accent} />
                  <Text className="text-xs font-bold text-academic-ink dark:text-[#EFEAE0]">Graph Filter Controls</Text>
                </View>

                {/* Depth Filter */}
                {onDepthChange ? (
                  <View className="gap-1.5">
                    <Text className="text-xs font-semibold text-academic-muted dark:text-[#A6A6AC]">Citation Lineage Depth</Text>
                    <View className="flex-row gap-2">
                      <Pressable
                        onPress={() => onDepthChange(1)}
                        className={`flex-1 items-center rounded-xl py-2 border ${
                          depth === 1 ? 'border-academic-maroon bg-academic-maroon dark:border-[#C6635E] dark:bg-[#C6635E]' : 'border-academic-gold/20'
                        }`}
                      >
                        <Text className={`text-xs font-bold ${depth === 1 ? 'text-white' : 'text-academic-ink dark:text-[#EFEAE0]'}`}>
                          1-Hop (Direct)
                        </Text>
                      </Pressable>
                      <Pressable
                        onPress={() => onDepthChange(2)}
                        className={`flex-1 items-center rounded-xl py-2 border ${
                          depth === 2 ? 'border-academic-maroon bg-academic-maroon dark:border-[#C6635E] dark:bg-[#C6635E]' : 'border-academic-gold/20'
                        }`}
                      >
                        <Text className={`text-xs font-bold ${depth === 2 ? 'text-white' : 'text-academic-ink dark:text-[#EFEAE0]'}`}>
                          2-Hop (Extended)
                        </Text>
                      </Pressable>
                    </View>
                  </View>
                ) : null}

                {/* Citation Threshold Filter */}
                {onMinCitationsChange ? (
                  <View className="gap-1.5">
                    <Text className="text-xs font-semibold text-academic-muted dark:text-[#A6A6AC]">Minimum Citation Threshold</Text>
                    <View className="flex-row gap-2">
                      {[0, 10, 50, 100].map((threshold) => (
                        <Pressable
                          key={threshold}
                          onPress={() => onMinCitationsChange(threshold)}
                          className={`flex-1 items-center rounded-xl py-1.5 border ${
                            minCitations === threshold
                              ? 'border-academic-navy bg-academic-navy dark:border-[#5D82AE] dark:bg-[#5D82AE]'
                              : 'border-academic-gold/20'
                          }`}
                        >
                          <Text
                            className={`text-xs font-bold ${
                              minCitations === threshold ? 'text-white' : 'text-academic-ink dark:text-[#EFEAE0]'
                            }`}
                          >
                            {threshold === 0 ? 'All' : `${threshold}+`}
                          </Text>
                        </Pressable>
                      ))}
                    </View>
                  </View>
                ) : null}
              </View>
            ) : null}

            {/* Paper Detail Content */}
            {isPaper && paperNode ? (
              <View className="gap-3">
                <View className="flex-row items-center gap-2">
                  <View className="rounded-full bg-academic-gold/20 px-2.5 py-1">
                    <Text className="text-xs font-bold text-academic-navy dark:text-[#5D82AE]">{paperNode.field}</Text>
                  </View>
                  {paperNode.is_center ? (
                    <View className="rounded-full bg-academic-maroon/20 px-2.5 py-1">
                      <Text className="text-xs font-bold text-academic-maroon dark:text-[#C6635E]">Selected Root</Text>
                    </View>
                  ) : null}
                </View>

                <Text className="font-serif text-xl font-bold text-academic-ink dark:text-[#EFEAE0]">
                  {paperNode.title}
                </Text>

                <View className="flex-row items-center gap-4">
                  <View className="flex-row items-center gap-1.5">
                    <Calendar size={14} color={colors.textSecondary} />
                    <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">{paperNode.year}</Text>
                  </View>
                  <View className="flex-row items-center gap-1.5">
                    <BookOpen size={14} color={colors.textSecondary} />
                    <Text className="text-xs font-bold text-academic-ink dark:text-[#EFEAE0]">
                      {paperNode.citation_count} citations
                    </Text>
                  </View>
                </View>

                {paperNode.abstract ? (
                  <View className="gap-1">
                    <Text className="text-xs font-bold uppercase text-academic-muted dark:text-[#A6A6AC]">Abstract Preview</Text>
                    <Text numberOfLines={4} className="text-sm leading-5 text-academic-ink dark:text-[#EFEAE0]">
                      {paperNode.abstract}
                    </Text>
                  </View>
                ) : null}

                {paperNode.authors && paperNode.authors.length > 0 ? (
                  <View className="gap-1">
                    <Text className="text-xs font-bold uppercase text-academic-muted dark:text-[#A6A6AC]">Authors</Text>
                    <Text className="text-xs text-academic-ink dark:text-[#EFEAE0]">
                      {paperNode.authors.map((a) => a.name).join(', ')}
                    </Text>
                  </View>
                ) : null}

                <Pressable
                  onPress={() => {
                    onClose();
                    router.push(`/paper/${paperNode.id}`);
                  }}
                  className="mt-2 flex-row items-center justify-center gap-2 rounded-xl bg-academic-maroon py-3.5 dark:bg-[#C6635E]"
                >
                  <ExternalLink size={16} color="#FBF9F4" />
                  <Text className="text-sm font-semibold text-academic-paper">View Full Paper Page</Text>
                </Pressable>
              </View>
            ) : null}

            {/* Author Detail Content */}
            {!isPaper && authorNode ? (
              <View className="gap-3">
                <View className="flex-row items-center gap-3">
                  <View className="h-14 w-14 items-center justify-center rounded-full bg-academic-navy dark:bg-[#5D82AE]">
                    <Text className="text-xl font-bold text-white">{authorNode.name.charAt(0)}</Text>
                  </View>
                  <View className="flex-1">
                    <Text className="text-lg font-bold text-academic-ink dark:text-[#EFEAE0]">{authorNode.name}</Text>
                    <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">
                      {[authorNode.institution, authorNode.field_of_study].filter(Boolean).join(' • ')}
                    </Text>
                  </View>
                </View>

                <View className="flex-row justify-between rounded-xl bg-white p-3 dark:bg-[#1F1F26]">
                  <View className="items-center">
                    <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">{authorNode.citation_count}</Text>
                    <Text className="text-[10px] text-academic-muted dark:text-[#A6A6AC]">Citations</Text>
                  </View>
                  <View className="items-center">
                    <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">{authorNode.h_index}</Text>
                    <Text className="text-[10px] text-academic-muted dark:text-[#A6A6AC]">h-index</Text>
                  </View>
                  <View className="items-center">
                    <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">{authorNode.i10_index}</Text>
                    <Text className="text-[10px] text-academic-muted dark:text-[#A6A6AC]">i10-index</Text>
                  </View>
                </View>

                <Pressable
                  onPress={() => {
                    onClose();
                    router.push(`/profile/${authorNode.id}`);
                  }}
                  className="mt-2 flex-row items-center justify-center gap-2 rounded-xl bg-academic-navy py-3.5 dark:bg-[#5D82AE]"
                >
                  <User size={16} color="#FFFFFF" />
                  <Text className="text-sm font-semibold text-white">View Researcher Profile</Text>
                </Pressable>
              </View>
            ) : null}
          </ScrollView>
        </SafeAreaView>
      </View>
    </Modal>
  );
}
