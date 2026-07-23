import { Award, BookOpen, TrendingUp, Zap } from 'lucide-react-native';
import React from 'react';
import { Text, View } from 'react-native';
import Svg, { Circle, Line, Path, Rect, Text as SvgText } from 'react-native-svg';

import { useAppTheme } from '@/components/ui/theme-provider';
import type { ResearcherAnalytics } from '@/types/graph';

interface ResearcherImpactCardProps {
  analytics: ResearcherAnalytics;
  authorName?: string;
}

export function ResearcherImpactCard({ analytics, authorName = 'Researcher' }: ResearcherImpactCardProps) {
  const { colors } = useAppTheme();
  const velocity = analytics.citation_velocity || [];

  const maxVal = Math.max(...velocity.map((v) => v.count), 1);
  const chartHeight = 80;
  const chartWidth = 280;
  const stepX = velocity.length > 1 ? chartWidth / (velocity.length - 1) : chartWidth;

  const points = velocity.map((v, i) => {
    const x = i * stepX;
    const y = chartHeight - (v.count / maxVal) * (chartHeight - 16) - 8;
    return { x, y, year: v.year, count: v.count };
  });

  const pathD = points.length > 0
    ? points.reduce((acc, p, i) => (i === 0 ? `M ${p.x} ${p.y}` : `${acc} L ${p.x} ${p.y}`), '')
    : '';

  return (
    <View className="overflow-hidden rounded-2xl border border-academic-gold/20 bg-white p-4 shadow-sm dark:border-[#33333D] dark:bg-[#1F1F26]">
      {/* Header */}
      <View className="flex-row items-center justify-between border-b border-academic-gold/15 pb-3 dark:border-[#33333D]">
        <View className="flex-row items-center gap-2">
          <View className="h-8 w-8 items-center justify-center rounded-xl bg-academic-gold/20 dark:bg-academic-gold/30">
            <TrendingUp size={18} color={colors.accent} />
          </View>
          <View>
            <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">Impact Analytics</Text>
            <Text className="text-xs text-academic-muted dark:text-[#A6A6AC]">{authorName}</Text>
          </View>
        </View>
        <View className="rounded-full bg-academic-navy/10 px-2.5 py-1 dark:bg-academic-navy/30">
          <Text className="text-xs font-bold text-academic-navy dark:text-[#5D82AE]">Cite Circle Verified</Text>
        </View>
      </View>

      {/* Summary Metrics */}
      <View className="mt-3 flex-row justify-between gap-2">
        <MetricBox icon={BookOpen} label="Total Citations" value={analytics.total_citations.toLocaleString()} color="#6C63FF" />
        <MetricBox icon={Award} label="h-index" value={String(analytics.h_index)} color="#2A9D8F" />
        <MetricBox icon={Zap} label="i10-index" value={String(analytics.i10-index)} color="#E76F51" />
      </View>

      {/* Citation Velocity Graph */}
      <View className="mt-4 gap-1.5">
        <View className="flex-row items-center justify-between">
          <Text className="text-xs font-bold uppercase text-academic-muted dark:text-[#A6A6AC]">
            Citation Velocity (Growth Over Time)
          </Text>
          <Text className="text-xs font-semibold text-academic-maroon dark:text-[#C6635E]">
            +{velocity[velocity.length - 1]?.count || 0} this year
          </Text>
        </View>

        <View className="mt-1 items-center rounded-xl bg-academic-parchment/60 p-3 dark:bg-[#15151A]">
          <Svg width={chartWidth} height={chartHeight + 20}>
            {/* Horizontal Grid lines */}
            <Line x1={0} y1={20} x2={chartWidth} y2={20} stroke="#C5A059" strokeWidth={0.5} opacity={0.2} />
            <Line x1={0} y1={60} x2={chartWidth} y2={60} stroke="#C5A059" strokeWidth={0.5} opacity={0.2} />

            {/* Sparkline Path */}
            {pathD ? <Path d={pathD} fill="none" stroke="#C6635E" strokeWidth={3} /> : null}

            {/* Points & Labels */}
            {points.map((p, idx) => (
              <G key={`point-${idx}`}>
                <Circle cx={p.x} cy={p.y} r={4} fill="#C6635E" stroke="#FFFFFF" strokeWidth={1.5} />
                <SvgText x={p.x} y={chartHeight + 16} fill={colors.textSecondary} fontSize={9} textAnchor="middle">
                  {p.year}
                </SvgText>
              </G>
            ))}
          </Svg>
        </View>
      </View>
    </View>
  );
}

function MetricBox({
  icon: IconComponent,
  label,
  value,
  color,
}: {
  icon: any;
  label: string;
  value: string;
  color: string;
}) {
  return (
    <View className="flex-1 items-center rounded-xl border border-academic-gold/15 bg-academic-paper p-2.5 dark:border-[#33333D] dark:bg-[#15151A]">
      <View className="mb-1 rounded-lg p-1.5" style={{ backgroundColor: `${color}18` }}>
        <IconComponent size={16} color={color} />
      </View>
      <Text className="text-base font-extrabold text-academic-ink dark:text-[#EFEAE0]">{value}</Text>
      <Text className="text-[10px] font-semibold text-academic-muted dark:text-[#A6A6AC]">{label}</Text>
    </View>
  );
}
