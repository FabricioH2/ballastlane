interface PaginationProps {
  page: number // starts at 0, like in the API; people see it starting at 1
  totalPages: number
  onChange: (newPage: number) => void
}

// Previous / next buttons.
export default function Pagination({ page, totalPages, onChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null
  }
  return (
    <div className="pagination">
      <button onClick={() => onChange(page - 1)} disabled={page === 0}>
        Previous
      </button>
      <span>
        Page {page + 1} of {totalPages}
      </span>
      <button onClick={() => onChange(page + 1)} disabled={page + 1 >= totalPages}>
        Next
      </button>
    </div>
  )
}
