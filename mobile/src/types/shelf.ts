/** Wire shape of GET /shelves — matches backend/schemas.py::Shelf. */
export interface Shelf {
  id: string;
  owner_id: string;
  name: string;
  description: string;
  paper_ids: string[];
}

/** Body for POST /shelves — matches backend/schemas.py::ShelfCreate. */
export interface ShelfCreate {
  name: string;
  description?: string;
}
