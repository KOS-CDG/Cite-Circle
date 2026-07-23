import { Compass, RefreshCw, ZoomIn, ZoomOut } from 'lucide-react-native';
import React, { useEffect, useMemo, useState } from 'react';
import { Dimensions, Pressable, Text, View } from 'react-native';
import Svg, { Circle, Defs, G, Line, Marker, Path, Text as SvgText } from 'react-native-svg';

import { useAppTheme } from '@/components/ui/theme-provider';
import type { CitationGraphEdge, CitationGraphNode } from '@/types/graph';

interface CitationGraphViewProps {
  nodes: CitationGraphNode[];
  edges: CitationGraphEdge[];
  selectedNodeId: string | null;
  onSelectNode: (node: CitationGraphNode) => void;
  depth: number;
  onDepthChange?: (newDepth: number) => void;
  width?: number;
  height?: number;
}

const FIELD_COLORS: Record<string, string> = {
  'Computer Science': '#6C63FF',
  'Artificial Intelligence & ML': '#6C63FF',
  'Human-Computer Interaction': '#00B4D8',
  'Physics': '#7209B7',
  'Quantum Computing & Physics': '#7209B7',
  'Life Sciences': '#2A9D8F',
  'Computational Biology': '#2A9D8F',
  'Meta-Research': '#E76F51',
  'Open Science & Meta-Research': '#E76F51',
  'General Research': '#5D82AE',
};

function getNodeColor(field: string): string {
  for (const [key, color] of Object.entries(FIELD_COLORS)) {
    if (field.toLowerCase().includes(key.toLowerCase())) return color;
  }
  return '#5D82AE';
}

function getNodeRadius(citationCount: number, isCenter: boolean): number {
  const base = isCenter ? 32 : Math.min(36, Math.max(18, 14 + Math.sqrt(citationCount) * 1.2));
  return base;
}

export function CitationGraphView({
  nodes,
  edges,
  selectedNodeId,
  onSelectNode,
  depth,
  onDepthChange,
  width: customWidth,
  height: customHeight,
}: CitationGraphViewProps) {
  const { colors } = useAppTheme();
  const screenWidth = Dimensions.get('window').width;
  const canvasWidth = customWidth || screenWidth - 32;
  const canvasHeight = customHeight || 420;

  const [scale, setScale] = useState<number>(0.9);
  const [pan, setPan] = useState<{ x: number; y: number }>({ x: 0, y: 0 });
  const [dragStart, setDragStart] = useState<{ x: number; y: number } | null>(null);

  // Force layout simulation refinement on incoming nodes
  const simulatedNodes = useMemo(() => {
    const nodeMap = new Map<string, { x: number; y: number }>();
    const nodeCount = nodes.length;
    if (nodeCount === 0) return [];

    nodes.forEach((node) => {
      let nx = node.x;
      let ny = node.y;
      if (node.is_center) {
        nx = 0;
        ny = 0;
      }
      nodeMap.set(node.id, { x: nx, y: ny });
    });

    // Simple spring-repulsion relaxation iterations for force layout
    for (let iter = 0; iter < 15; iter++) {
      nodes.forEach((n1, i) => {
        nodes.forEach((n2, j) => {
          if (i >= j) return;
          const p1 = nodeMap.get(n1.id)!;
          const p2 = nodeMap.get(n2.id)!;
          const dx = p2.x - p1.x;
          const dy = p2.y - p1.y;
          const dist = Math.sqrt(dx * dx + dy * dy) || 1;
          const minDist = 110;
          if (dist < minDist) {
            const force = (minDist - dist) / dist * 0.2;
            if (!n1.is_center) {
              p1.x -= dx * force;
              p1.y -= dy * force;
            }
            if (!n2.is_center) {
              p2.x += dx * force;
              p2.y += dy * force;
            }
          }
        });
      });
    }

    return nodes.map((node) => {
      const pos = nodeMap.get(node.id) || { x: node.x, y: node.y };
      return {
        ...node,
        computedX: pos.x + canvasWidth / 2,
        computedY: pos.y + canvasHeight / 2,
      };
    });
  }, [nodes, canvasWidth, canvasHeight]);

  const nodePosMap = useMemo(() => {
    const map = new Map<string, { x: number; y: number; r: number }>();
    simulatedNodes.forEach((n) => {
      map.set(n.id, {
        x: n.computedX,
        y: n.computedY,
        r: getNodeRadius(n.citation_count, n.is_center),
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

      {/* Depth Filter Toggle */}
      {onDepthChange ? (
        <View className="absolute left-3 top-3 z-10 flex-row items-center gap-1 rounded-xl bg-white/90 p-1 shadow-sm backdrop-blur-md dark:bg-[#252530]/90">
          <Pressable
            onPress={() => onDepthChange(1)}
            className={`rounded-lg px-2.5 py-1 ${depth === 1 ? 'bg-academic-maroon dark:bg-[#C6635E]' : ''}`}
          >
            <Text className={`text-xs font-bold ${depth === 1 ? 'text-white' : 'text-academic-muted dark:text-[#A6A6AC]'}`}>
              1-Hop
            </Text>
          </Pressable>
          <Pressable
            onPress={() => onDepthChange(2)}
            className={`rounded-lg px-2.5 py-1 ${depth === 2 ? 'bg-academic-maroon dark:bg-[#C6635E]' : ''}`}
          >
            <Text className={`text-xs font-bold ${depth === 2 ? 'text-white' : 'text-academic-muted dark:text-[#A6A6AC]'}`}>
              2-Hop
            </Text>
          </Pressable>
        </View>
      ) : null}

      {/* Interactive Canvas View */}
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
          <Defs>
            <Marker id="arrow" viewBox="0 0 10 10" refX="18" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
              <Path d="M 0 0 L 10 5 L 0 10 z" fill="#B5651D" />
            </Marker>
            <Marker id="arrow-selected" viewBox="0 0 10 10" refX="18" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
              <Path d="M 0 0 L 10 5 L 0 10 z" fill="#C6635E" />
            </Marker>
          </Defs>

          <G transform={`translate(${pan.x}, ${pan.y}) scale(${scale})`}>
            {/* Concentric Hop Rings */}
            <Circle cx={canvasWidth / 2} cy={canvasHeight / 2} r={180} stroke="#C5A059" strokeWidth={1} strokeDasharray="4, 4" opacity={0.25} />
            <Circle cx={canvasWidth / 2} cy={canvasHeight / 2} r={340} stroke="#C5A059" strokeWidth={1} strokeDasharray="4, 4" opacity={0.15} />

            {/* Citation Edges (Arrow pointing from citing paper to cited paper) */}
            {edges.map((edge, idx) => {
              const src = nodePosMap.get(edge.source);
              const tgt = nodePosMap.get(edge.target);
              if (!src || !tgt) return null;

              const isEdgeHighlighted = selectedNodeId === edge.source || selectedNodeId === edge.target;
              const dx = tgt.x - src.x;
              const dy = tgt.y - src.y;
              const dist = Math.sqrt(dx * dx + dy * dy) || 1;
              const normX = dx / dist;
              const normY = dy / dist;

              // Offset start and end to stop at node boundaries
              const startX = src.x + normX * src.r;
              const startY = src.y + normY * src.r;
              const endX = tgt.x - normX * (tgt.r + 6);
              const endY = tgt.y - normY * (tgt.r + 6);

              return (
                <G key={`edge-${idx}`}>
                  <Line
                    x1={startX}
                    y1={startY}
                    x2={endX}
                    y2={endY}
                    stroke={isEdgeHighlighted ? '#C6635E' : '#B5651D'}
                    strokeWidth={isEdgeHighlighted ? 2.5 : 1.5}
                    strokeDasharray={edge.type === 'CITES' ? 'none' : '3, 3'}
                    opacity={isEdgeHighlighted ? 0.95 : 0.4}
                    markerEnd={isEdgeHighlighted ? 'url(#arrow-selected)' : 'url(#arrow)'}
                  />
                </G>
              );
            })}

            {/* Paper Nodes */}
            {simulatedNodes.map((node) => {
              const isSelected = selectedNodeId === node.id;
              const radius = getNodeRadius(node.citation_count, node.is_center);
              const nodeColor = getNodeColor(node.field);

              return (
                <G key={node.id} transform={`translate(${node.computedX}, ${node.computedY})`}>
                  {/* Outer aura for center / selected */}
                  {node.is_center || isSelected ? (
                    <Circle
                      cx={0}
                      cy={0}
                      r={radius + 8}
                      fill={node.is_center ? '#D4AF37' : colors.accent}
                      opacity={0.3}
                    />
                  ) : null}

                  {/* Main Node Body */}
                  <Circle
                    cx={0}
                    cy={0}
                    r={radius}
                    fill={nodeColor}
                    stroke={node.is_center ? '#D4AF37' : isSelected ? colors.accent : '#FFFFFF'}
                    strokeWidth={node.is_center ? 3 : isSelected ? 3 : 1.5}
                    onPress={() => onSelectNode(node)}
                  />

                  {/* Hop badge / Citation Count */}
                  <SvgText
                    x={0}
                    y={node.is_center ? -2 : 3}
                    fill="#FFFFFF"
                    fontSize={node.is_center ? 12 : 10}
                    fontWeight="bold"
                    textAnchor="middle"
                  >
                    {node.citation_count}
                  </SvgText>

                  {/* Paper Title Label */}
                  <SvgText
                    x={0}
                    y={radius + 14}
                    fill={colors.text}
                    fontSize={10}
                    fontWeight={node.is_center || isSelected ? 'bold' : '500'}
                    textAnchor="middle"
                  >
                    {node.title.length > 18 ? `${node.title.slice(0, 16)}…` : node.title}
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
