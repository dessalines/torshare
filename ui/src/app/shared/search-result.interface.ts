export interface SearchResult {
	id: number;
	info_hash: string;
	path: string;
	index_: number;
	age: number;
	created: number;
	peers: number;
	seeders: number;
	size_bytes: number;
}