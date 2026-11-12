// The Matriz logo: three equal, rounded strokes at 45 degrees, rising from left to right like "///", in the three
// status colours: To do (blue), In progress (orange) and Done (green). Original artwork; the colours come from the
// tokens, so it follows the theme.

const STROKES = [{ x: 3.5, colour: "var(--todo)" }, { x: 11, colour: "var(--progress)" }, { x: 18.5, colour: "var(--done)" }];

export function LogoMark({ size = 28 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true" focusable="false">
      <g strokeWidth="4.6" strokeLinecap="round" fill="none" transform="translate(-0.5 -2.5)">
        {STROKES.map((stroke) => (
          <line key={stroke.x} x1={stroke.x} y1="24.5" x2={stroke.x + 11} y2="13.5" stroke={stroke.colour} />
        ))}
      </g>
    </svg>
  );
}

type Props = { large?: boolean; href?: string };

/** The mark and the word "matriz"; a link to the home dashboard when `href` is given. */
export function Logo({ large = false, href }: Props) {
  const content = (
    <>
      <LogoMark size={large ? 56 : 28} />
      <span className="logo-word">matriz</span>
    </>
  );
  const className = large ? "logo logo-large" : "logo";
  return href
    ? <a className={className} href={href} aria-label="Matriz, my projects">{content}</a>
    : <span className={className} role="img" aria-label="Matriz">{content}</span>;
}
