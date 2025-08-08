import type { PropsWithChildren } from "react";
import styles from "./index.module.scss";
import type React from "react";

interface IProgressButtonProps extends PropsWithChildren {
  style?: React.CSSProperties;
  onClick?: () => void;
  disabled?: boolean;
  progress: number;
}

export default function ProgressButton({
  children,
  style,
  disabled,
  progress,
  onClick,
}: IProgressButtonProps) {
  return (
    <div
      className={styles.btn}
      style={{ ...style, opacity: disabled ? 0.8 : 1 }}
      onClick={onClick}
    >
      <div className={styles.block} style={{ width: `${progress}%` }}></div>
      <div className={styles.child}>{children}</div>
    </div>
  );
}
