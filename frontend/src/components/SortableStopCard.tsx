import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import type { ReactNode } from 'react'

interface SortableStopCardProps {
  stopId: number
  disabled: boolean
  recentlyMoved: boolean
  className: string
  children: (dragHandle: ReactNode) => ReactNode
}

export function SortableStopCard({
  stopId,
  disabled,
  recentlyMoved,
  className,
  children,
}: SortableStopCardProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: stopId, disabled })

  const dragHandle = disabled ? null : (
    <button
      type="button"
      className="drag-handle"
      aria-label="끌어서 배송 순서 변경"
      {...attributes}
      {...listeners}
    >
      <span aria-hidden="true">⠿</span>
      끌어서 이동
    </button>
  )

  return (
    <article
      ref={setNodeRef}
      className={`${className} sortable-stop-card${isDragging ? ' is-dragging' : ''}${recentlyMoved ? ' just-moved' : ''}`}
      style={{
        transform: CSS.Transform.toString(transform),
        transition,
      }}
    >
      {children(dragHandle)}
    </article>
  )
}
