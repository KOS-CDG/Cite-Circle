import { Compass, Users, ZoomIn, ZoomOut } from 'lucide-react-native';
import React, { useMemo, useState } from 'react';
import { Dimensions, Image as RNImage, Pressable, Text, View } from 'react-native';
import Svg, { Circle, G, Line, Path, Rect, Text as SvgText } from 'react-native-svg';

import { useAppTheme } from '@/components/ui/theme-provider';
import type { CoauthorCluster, CoauthorGraphEdge, CoauthorGraphNode } from '@/types/graph';

interface CoAuthorNetworkViewProps {
  nodes: CoauthorGraphNode[];
  edges: CoauthorGraphEdge[];
  clusters: CoauthorCluster[];
  selectedNodeId: string | null;
  onSelectNode: (node: CoauthorGraphNode) => void;
  width?: number;
  height?: number;
}

export function CoAuthorNetworkView({
  nodes,
  edges,
  clusters,
  selectedNodeId,
  onSelectNode,
  width: customWidth,
  height: customHeight,
}: CoAuthorNetworkViewProps) {
  const { colors } = useAppTheme();
  const screenWidth = Dimensions.get('window').width;
  const canvasWidth = customWidth || screenWidth - 32;
  const canvasHeight = customHeight || 420;

  const [scale, setScale] = useState<number>(0.9);
  const [pan, setPan] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [dragStart, setDragStart] = useState<{ x: number; y: number } | null>(null);

  const clusterColorMap = useMemo(() => {
    const map = new Map<string, string>();
    clusters.forEach((c) => map.set(c.id, c.color));
    return map;
  }, [clusters]);

  const simulatedNodes = useMemo(() => {
    return nodes.map((node) => ({
      ...node,
      computedX: node.x + canvasWidth / 2,
      computedY: node.y + canvasHeight / 2,
    }));
  }, [nodes, canvasWidth, canvasHeight]);

  const nodePosMap = useMemo(() => {
    const map = new Map<string, { x: number; y: number; r: number }>();
    simulatedNodes.forEach((n) => {
      map.set(n.id, {
        x: n.computedX,
        y: n.computedY,
        r: n.is_center ? 28 : 22,
      });
    });
    return map;
  }, [simulatedNodes]);

  const handleZoomIn = () => setScale((s) => Math.min(2.5, s + 0.2));
  const handleZoomOut = () => setScale((s) => Math.max(0.4, s - 0.2));
  const handleResetPan = () => {
    setScale(0.9);
    setPan({ x: 0, y: 0 });
  };

  return (
    <View className="relative overflow-hidden rounded-2xl border border-academic-gold/20 bg-academic-parchment/60 dark:border-[#33333D] dark:bg-[#191920]">
      {/* Control overlay */}
      <View className="absolute right-3 top-3 z-10 flex-col gap-2 rounded-xl bg-white/90 p-1.5 shadow-sm backdrop-blur-md dark:bg-[#252530]/90">
        <Pressable onPress={handleZoomIn} className="p-2">
          <ZoomIn size={18} color={colors.text} />
        </Pressable>
        <Pressable onPress={handleZoomOut} className="p-2">
          <ZoomOut size={18} color={colors.text} />
        </Pressable>
        <Pressable onPress={handleResetPan} className="p-2">
          <Compass size={18} color={colors.accent} />
        </Pressable>
      </View>

      {/* Cluster Badges Header */}
      <View className="absolute left-3 top-3 z-10 flex-row flex-wrap gap-1.5 max-w-[70%]">
        {clusters.map((cluster) => (
          <View
            key={cluster.id}
            className="flex-row items-center gap-1 rounded-lg px-2 py-0.5"
            style={{ backgroundColor: `${cluster.color}22`, borderColor: `${cluster.color}55`, borderWidth: 1 }}
          >
            <View className="h-2 w-2 rounded-full" style={{ backgroundColor: cluster.color }} />
            <Text className="text-[10px] font-bold" style={{ color: cluster.color }}>
              {cluster.name}
            </Text>
          </View>
        ))}
      </View>

      {/* Interactive Network View */}
      <View
        style={{ width: canvasWidth, height: canvasHeight }}
        onStartShouldSetResponder={() => true}
        onResponderGrant={(evt) => {
          const touch = evt.nativeEvent;
          setDragStart({ x: touch.pageX - pan.x, y: touch.pageY - pan.y });
        }}
        onResponderMove={(evt) => {
          if (dragStart) {
            const touch = evt.nativeEvent;
            setPan({ x: touch.pageX - dragStart.x, y: touch.pageY - dragStart.y });
          }
        }}
        onResponderRelease={() => setDragStart(null)}
      >
        <Svg width={canvasWidth} height={canvasHeight}>
          <G transform={`translate(${pan.x}, ${pan.y}) scale(${scale})`}>
            {/* Cluster Hulls / Background Areas */}
            {clusters.map((cluster) => {
              const members = simulatedNodes.filter((n) => cluster.member_ids.includes(n.id));
              if (members.length < 2) return null;
              const avgX = members.reduce((acc, m) => acc + m.computedX, 0) / members.length;
              const avgY = members.reduce((acc, m) => acc + m.computedY, 0) / members.length;
              const hullRadius = 110;

              return (
                <G key={`cluster-hull-${cluster.id}`}>
                  <Circle
                    cx={avgX}
                    cy={avgY}
                    r={hullRadius}
                    fill={cluster.color}
                    opacity={0.08}
                    stroke={cluster.color}
                    strokeWidth={1}
                    strokeDasharray="4, 4"
                  />
                </G>
              );
            })}

            {/* Co-Author Weighted Edges */}
            {edges.map((edge, idx) => {
              const src = nodePosMap.get(edge.source);
              const tgt = nodePosMap.get(edge.target);
              if (!src || !tgt) return null;

              const isEdgeHighlighted = selectedNodeId === edge.source || selectedNodeId === edge.target;
              const strokeWidth = Math.min(6, 1.5 + edge.weight * 1.2);

              return (
                <G key={`coauthor-edge-${idx}`}>
                  <Line
                    x1={src.x}
                    y1={src.y}
                    x2={tgt.x}
                    y2={tgt.y}
                    stroke={isEdgeHighlighted ? '#C6635E' : '#6C63FF'}
                    strokeWidth={strokeWidth}
                    opacity={isEdgeHighlighted ? 0.9 : 0.45}
                  />
                </G>
              );
            })}

            {/* Researcher Nodes */}
            {simulatedNodes.map((node) => {
              const isSelected = selectedNodeId === node.id;
              const radius = node.is_center ? 28 : 22;
              const clusterColor = clusterColorMap.get(node.cluster_id) || '#6C63FF';

              return (
                <G key={node.id} transform={`translate(${node.computedX}, ${node.computedY})`}>
                  {/* Outer ring */}
                  <Circle
                    cx={0}
                    cy={0}
                    r={radius + (node.is_center || isSelected ? 6 : 2)}
                    fill={clusterColor}
                    opacity={node.is_center ? 0.25 : 0.15}
                  />

                  {/* Main Node Body */}
                  <Circle
                    cx={0}
                    cy={0}
                    r={radius}
                    fill={clusterColor}
                    stroke={node.is_center ? '#D4AF37' : isSelected ? colors.accent : '#FFFFFF'}
                    strokeWidth={node.is_center ? 3 : isSelected ? 3 : 2}
                    onPress={() => onSelectNode(node)}
                  />

                  {/* Researcher Initial */}
                  <SvgText
                    x={0}
                    y={5}
                    fill="#FFFFFF"
                    fontSize={node.is_center ? 14 : 12}
                    fontWeight="bold"
                    textAnchor="middle"
                  >
                    {node.name.charAt(0).toUpperCase()}
                  </SvgText>

                  {/* Name Label */}
                  <SvgText
                    x={0}
                    y={radius + 14}
                    fill={colors.text}
                    fontSize={10}
                    fontWeight={node.is_center || isSelected ? 'bold' : '600'}
                    textAnchor="middle"
                  >
                    {node.name}
                  </SvgText>
                  <SvgText
                    x={0}
                    y={radius + 26}
                    fill={colors.textSecondary}
                    fontSize={8}
                    textAnchor="middle"
                  >
                    {`h-index: ${node.h_index}`}
                  </SvgText>
                </G>
              );
            })}
          </G>
        </Svg>
      </View>
    </View>
  );
}
