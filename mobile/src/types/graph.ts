import type { User } from './user';

export interface CitationGraphNode {
  id: string;
  title: string;
  abstract: string;
  citation_count: number;
  year: number;
  circle_id?: string | null;
  field: string;
  authors: User[];
  doi: string;
  journal: string;
  is_center: boolean;
  hop_distance: number;
  x: number;
  y: number;
}

export interface CitationGraphEdge {
  source: string;
  target: string;
  type: string;
}

export interface CitationGraphSummary {
  total_papers: number;
  total_citations: number;
  max_depth: number;
}

export interface CitationGraphResponse {
  nodes: CitationGraphNode[];
  edges: CitationGraphEdge[];
  summary: CitationGraphSummary;
}

export interface CoauthorGraphNode {
  id: string;
  name: string;
  avatar_url: string;
  institution: string;
  field_of_study: string;
  citation_count: number;
  h_index: number;
  i10_index: number;
  cluster_id: string;
  is_center: boolean;
  x: number;
  y: number;
}

export interface CoauthorGraphEdge {
  source: string;
  target: string;
  weight: number;
  publications: string[];
}

export interface CoauthorCluster {
  id: string;
  name: string;
  color: string;
  member_ids: string[];
}

export interface CitationVelocityPoint {
  year: number;
  count: number;
}

export interface ResearcherAnalytics {
  total_citations: number;
  h_index: number;
  i10_index: number;
  citation_velocity: CitationVelocityPoint[];
}

export interface CoauthorGraphResponse {
  nodes: CoauthorGraphNode[];
  edges: CoauthorGraphEdge[];
  clusters: CoauthorCluster[];
  analytics: ResearcherAnalytics;
}
