import type { PropsWithChildren } from 'react'
import React from 'react'

interface IButtonProps {
  style?: React.CSSProperties
  onClick?: () => void
  disabled?: boolean
  width?: React.CSSProperties['width']
  height?: React.CSSProperties['height']
  size?: 'small' | 'medium' | 'large',
  type?: "submit" | "reset" | "button" | undefined;
}

export function Button({
  children,
  style,
  onClick,
  disabled,
  size,
  width,
  height,
  type,
}: PropsWithChildren<IButtonProps>) {
  return (
    <button
      type={type}
      className={`btn-primary ${size}`}
      style={{ width, height, ...style }}
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  )
}
