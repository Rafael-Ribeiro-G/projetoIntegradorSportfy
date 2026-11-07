import type { Esporte } from "./esporte";
import type { LocalResumo, QuadraResumo, TipoLocal } from "./local";
import type { NivelHabilidade, UsuarioResumo } from "./usuario";

/** Use esta na tela (e não `status`). */
export type SituacaoPartida = "ABERTA" | "LOTADA" | "EM_ANDAMENTO" | "ENCERRADA" | "CANCELADA";

/** Valor gravado no banco. */
export type StatusPartida = "ABERTA" | "CANCELADA" | "FINALIZADA";

/** Partida = evento esportivo. Datas no formato "2026-10-25T19:00:00" (sem fuso). */
export interface Partida {
  id: string;
  titulo?: string;
  descricao?: string;
  imagemUrl?: string;
  esporte: Esporte;
  /** Some quando o evento não usa quadra. */
  quadra?: QuadraResumo;
  local: LocalResumo;
  criador: UsuarioResumo;
  dataHoraInicio: string;
  dataHoraFim: string;
  duracaoMinutos?: number;
  maxJogadores?: number;
  participantesConfirmados?: number;
  /** Some quando o evento não tem limite de pessoas. */
  vagasRestantes?: number;
  status?: StatusPartida;
  situacao: SituacaoPartida;
  /** Só vem quando a busca manda latitude/longitude. */
  distanciaKm?: number;
  /** Só vem quando a requisição tem token. */
  usuarioParticipa?: boolean;
}

/** Filtros de GET /api/partidas. Todos opcionais. */
export interface FiltrosPartidas {
  esporteId?: number;
  localId?: string;
  quadraId?: string;
  tipoLocal?: TipoLocal;
  texto?: string;
  dataInicio?: string;
  dataFim?: string;
  latitude?: number;
  longitude?: number;
  raioKm?: number;
  incluirEncerradas?: boolean;
  incluirCanceladas?: boolean;
  /** Começa em 0. */
  pagina?: number;
  /** 1 a 50 (padrão 20). */
  tamanho?: number;
}

/** Parâmetros de GET /api/partidas/sugestoes. */
export interface FiltrosSugestoes {
  latitude?: number;
  longitude?: number;
  esporteId?: number;
  limite?: number;
}

/**
 * POST/PUT /api/partidas.
 * Mande `quadraId` (local e esporte vêm da quadra) OU `localId` + `esporteId`.
 */
export interface PartidaRequest {
  quadraId?: string;
  localId?: string;
  esporteId?: number;
  /** Até 100 caracteres. null = sem título. */
  titulo?: string | null;
  /** Até 1000 caracteres. */
  descricao?: string | null;
  /** Link http/https (use a `url` do upload). */
  imagemUrl?: string | null;
  dataHoraInicio: string;
  dataHoraFim: string;
  /** 2 a 100. Sem ele, usa o padrão do esporte (que pode ser "sem limite"). */
  maxJogadores?: number | null;
}

/** GET /api/partidas/{id}/participantes */
export interface Participante {
  id: string;
  nome: string;
  fotoUrl?: string;
  nivelHabilidade?: NivelHabilidade;
  criador?: boolean;
}

/** GET /api/usuarios/me/partidas */
export interface MinhasPartidas {
  criadas: Partida[];
  participando: Partida[];
}
