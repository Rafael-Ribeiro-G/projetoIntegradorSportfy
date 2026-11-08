// Ilustração de fundo das telas de entrada: linhas de um campo de futebol + algumas bolas.
// É um SVG feito à mão (sem foto, sem direitos autorais) que usa "currentColor":
// a cor vem da classe (text-primary), então funciona no tema claro e no escuro.

/** Bola de futebol simples: círculo + pentágono no meio + costuras até a borda. */
function Bola({ x, y, r }: { x: number; y: number; r: number }) {
  const vertices = Array.from({ length: 5 }, (_, i) => {
    const angulo = (-90 + i * 72) * (Math.PI / 180);
    return { px: x + Math.cos(angulo) * r * 0.38, py: y + Math.sin(angulo) * r * 0.38, angulo };
  });
  const pentagono = vertices.map((v) => `${v.px.toFixed(1)},${v.py.toFixed(1)}`).join(" ");

  return (
    <g>
      <circle cx={x} cy={y} r={r} />
      <polygon points={pentagono} fill="currentColor" stroke="none" />
      {vertices.map((v, i) => (
        <line
          key={i}
          x1={v.px}
          y1={v.py}
          x2={x + Math.cos(v.angulo) * r}
          y2={y + Math.sin(v.angulo) * r}
        />
      ))}
    </g>
  );
}

export function FundoEsportivo({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden
      viewBox="0 0 1200 800"
      preserveAspectRatio="xMidYMid slice"
      fill="none"
      stroke="currentColor"
      strokeWidth={4}
      strokeLinecap="round"
      className={`pointer-events-none absolute inset-0 h-full w-full ${className}`}
    >
      {/* Campo levemente inclinado, para dar movimento */}
      <g transform="rotate(-8 600 400)">
        <rect x="150" y="100" width="900" height="600" rx="10" />
        <line x1="600" y1="100" x2="600" y2="700" />
        <circle cx="600" cy="400" r="90" />
        <circle cx="600" cy="400" r="6" fill="currentColor" />

        {/* Área esquerda */}
        <rect x="150" y="240" width="150" height="320" />
        <rect x="150" y="325" width="60" height="150" />
        <circle cx="255" cy="400" r="5" fill="currentColor" />
        <path d="M300 340 A 80 80 0 0 1 300 460" />

        {/* Área direita */}
        <rect x="900" y="240" width="150" height="320" />
        <rect x="990" y="325" width="60" height="150" />
        <circle cx="945" cy="400" r="5" fill="currentColor" />
        <path d="M900 340 A 80 80 0 0 0 900 460" />

        {/* Escanteios */}
        <path d="M150 125 A 25 25 0 0 0 175 100" />
        <path d="M1025 100 A 25 25 0 0 0 1050 125" />
        <path d="M150 675 A 25 25 0 0 1 175 700" />
        <path d="M1025 700 A 25 25 0 0 1 1050 675" />
      </g>

      {/* Bolas espalhadas nas bordas */}
      <Bola x={90} y={110} r={34} />
      <Bola x={1110} y={150} r={24} />
      <Bola x={1090} y={700} r={44} />
      <Bola x={120} y={690} r={26} />
    </svg>
  );
}
