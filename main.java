import java.util.Random;
import java.util.Scanner;

// Classe No
// Estrutura para lista encadeada simples.
// Armazena a chave e a referência para o próximo nó.
class No {
    public int chave;
    public No proximo;

    public No(int chave, No proximo) {
        this.chave = chave;
        this.proximo = proximo;
    }
}

// Classe Main
// Implementação de Tabela Hash com Encadeamento Separado.
// Realiza experimentos comparando funções de hash (Divisão, Multiplicação, Dobramento)
// sob diferentes fatores de carga e tamanhos de tabela.
// Gera saída em formato CSV.
public class Main {

    static final int QTD_M = 3;
    static final int QTD_N = 3;
    static final int QTD_SEEDS = 3;
    static final int QTD_FUNC = 3;

    static final double A_CONSTANTE = 0.6180339887;

    static final String H_DIV = "H_DIV";
    static final String H_MUL = "H_MUL";
    static final String H_FOLD = "H_FOLD";

    // Método main
    // Configura os parâmetros do experimento (vetores de tamanhos, seeds e funções).
    // Imprime o cabeçalho do CSV.
    // Executa os loops aninhados para cobrir todas as combinações de testes.
    public static void main(String[] args) {
        int[] vetorM = {1009, 10007, 100003};
        int[] vetorN = {1000, 10000, 100000};
        long[] vetorSeeds = {137L, 271828L, 314159L};
        String[] vetorFunc = {H_DIV, H_MUL, H_FOLD};

        System.out.println("m,n,func,seed,ins_ms,coll_tbl,coll_lst,find_ms_hits,find_ms_misses,cmp_hits,cmp_misses,checksum");

        int iM = 0;
        while (iM < QTD_M) {
            int m = vetorM[iM];

            int iFunc = 0;
            while (iFunc < QTD_FUNC) {
                String func = vetorFunc[iFunc];

                int iN = 0;
                while (iN < QTD_N) {
                    int n = vetorN[iN];

                    int iSeed = 0;
                    while (iSeed < QTD_SEEDS) {
                        long seed = vetorSeeds[iSeed];
                        rodarTeste(m, n, func, seed);
                        iSeed = iSeed + 1;
                    }
                    iN = iN + 1;
                }
                iFunc = iFunc + 1;
            }
            iM = iM + 1;
        }
    }

    // Método rodarTeste
    // Executa uma instância do experimento:
    // 1. Imprime auditoria (System.err).
    // 2. Gera dados determinísticos.
    // 3. Insere dados medindo tempo e colisões (Tabela e Lista).
    // 4. Calcula checksum dos primeiros 10 índices.
    // 5. Realiza buscas (Hits e Misses) medindo tempo e comparações.
    // 6. Imprime resultados em CSV.
    static void rodarTeste(int m, int n, String func, long seed) {
        System.err.println("AUDITORIA: " + func + " m=" + m + " seed=" + seed);

        No[] tabela = new No[m];
        int[] dados = gerarDados(n, seed);

        long inicioIns = System.nanoTime();
        long colsTabela = 0;
        long colsLista = 0;
        long checksum = 0;

        int i = 0;
        while (i < n) {
            int chave = dados[i];
            int idx = hash(chave, m, func);

            if (i < 10) {
                checksum = checksum + idx;
            }

            if (tabela[idx] == null) {
                tabela[idx] = new No(chave, null);
            } else {
                colsTabela = colsTabela + 1;
                No atual = tabela[idx];
                while (atual.proximo != null) {
                    colsLista = colsLista + 1;
                    atual = atual.proximo;
                }
                colsLista = colsLista + 1;
                atual.proximo = new No(chave, null);
            }
            i = i + 1;
        }

        long fimIns = System.nanoTime();
        double tempoIns = (fimIns - inicioIns) / 1000000.0;
        long checksumFinal = checksum % 1000003;

        int[] busca = prepararBusca(dados, n, seed);

        long somaCmpHits = 0;
        long somaCmpMisses = 0;
        long somaTempoHits = 0;
        long somaTempoMisses = 0;

        int rep = 0;
        while (rep < 5) {
            long tHits = 0;
            long tMisses = 0;
            long cHits = 0;
            long cMisses = 0;

            int j = 0;
            while (j < n) {
                int chaveBusca = busca[j];
                int idx = hash(chaveBusca, m, func);
                long t0 = System.nanoTime();

                No atual = tabela[idx];
                boolean achou = false;
                long cmp = 0;

                while (atual != null) {
                    cmp = cmp + 1;
                    if (atual.chave == chaveBusca) {
                        achou = true;
                        atual = null;
                    } else {
                        atual = atual.proximo;
                    }
                }
                long t1 = System.nanoTime();

                if (achou) {
                    tHits = tHits + (t1 - t0);
                    cHits = cHits + cmp;
                } else {
                    tMisses = tMisses + (t1 - t0);
                    cMisses = cMisses + cmp;
                }
                j = j + 1;
            }

            somaTempoHits = somaTempoHits + tHits;
            somaTempoMisses = somaTempoMisses + tMisses;
            if (rep == 4) {
                somaCmpHits = cHits;
                somaCmpMisses = cMisses;
            }
            rep = rep + 1;
        }

        double mediaTempoHits = (somaTempoHits / 5.0) / 1000000.0;
        double mediaTempoMisses = (somaTempoMisses / 5.0) / 1000000.0;

        System.out.println(m + "," + n + "," + func + "," + seed + ","
                + String.format("%.4f", tempoIns).replace(',', '.') + ","
                + colsTabela + "," + colsLista + ","
                + String.format("%.4f", mediaTempoHits).replace(',', '.') + ","
                + String.format("%.4f", mediaTempoMisses).replace(',', '.') + ","
                + somaCmpHits + "," + somaCmpMisses + "," + checksumFinal);
    }

    // Método gerarDados
    // Gera um vetor de inteiros aleatórios de 9 dígitos com base na seed.
    static int[] gerarDados(int n, long seed) {
        Random rnd = new Random(seed);
        int[] d = new int[n];
        int i = 0;
        while (i < n) {
            d[i] = 100000000 + rnd.nextInt(900000000);
            i = i + 1;
        }
        return d;
    }

    // Método prepararBusca
    // Gera um vetor misto para testes de busca:
    // 50% chaves presentes (hits) e 50% chaves aleatórias novas (misses).
    // Embaralha o vetor resultante.
    static int[] prepararBusca(int[] originais, int n, long seed) {
        Random rnd = new Random(seed + 1);
        int[] b = new int[n];
        int i = 0;
        while (i < n) {
            if (i % 2 == 0) {
                b[i] = originais[i];
            } else {
                b[i] = 100000000 + rnd.nextInt(900000000);
            }
            i = i + 1;
        }

        int k = 0;
        while (k < n) {
            int r = k + rnd.nextInt(n - k);
            int tmp = b[r];
            b[r] = b[k];
            b[k] = tmp;
            k = k + 1;
        }
        return b;
    }

    // Método hash
    // Seletor centralizado para chamar a função de hash correta.
    static int hash(int chave, int m, String tipo) {
        if (tipo.equals(H_DIV)) return hDiv(chave, m);
        if (tipo.equals(H_MUL)) return hMul(chave, m);
        return hFold(chave, m);
    }

    // Método hDiv (Divisão)
    // Retorna o resto da divisão da chave pelo tamanho da tabela.
    static int hDiv(int k, int m) {
        return k % m;
    }

    // Método hMul (Multiplicação)
    // Utiliza a constante áurea para calcular o índice.
    // Extrai a parte fracionária manualmente para evitar Math.floor.
    static int hMul(int k, int m) {
        double val = k * A_CONSTANTE;
        double frac = val - (long) val;
        return (int) (frac * m);
    }

    // Método hFold (Dobramento)
    // Soma blocos de 3 dígitos da chave.
    static int hFold(int k, int m) {
        int soma = 0;
        int temp = k;
        while (temp > 0) {
            soma = soma + (temp % 1000);
            temp = temp / 1000;
        }
        return soma % m;
    }
}
