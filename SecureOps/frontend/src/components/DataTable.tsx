import type { ReactNode } from 'react';
import { EmptyState } from './states';

export interface Column<T> { key: string; header: string; render: (row: T) => ReactNode; align?: 'left' | 'right' }

export default function DataTable<T>({ columns, rows, rowKey, emptyTitle = 'Nothing to show', caption }: {
  columns: Column<T>[]; rows: T[]; rowKey: (row: T) => string | number; emptyTitle?: string; caption?: string;
}) {
  if (rows.length === 0) return <EmptyState title={emptyTitle} />;
  return (
    <div className="table-wrap">
      <table className="table">
        {caption && <caption className="sr-only">{caption}</caption>}
        <thead><tr>{columns.map((c) => <th key={c.key} scope="col" className={c.align === 'right' ? 'right' : ''}>{c.header}</th>)}</tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={rowKey(r)}>{columns.map((c) => <td key={c.key} className={c.align === 'right' ? 'right' : ''}>{c.render(r)}</td>)}</tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
