# Regeneration plan

Baseline (read-only): `/Users/gary/_experiment/logback/experiments/commit-survival`

| Group | Trigger | Tag | Requested index | Evaluable index | Baseline alive / denominator | Missing scope classes |
|---|---|---|---:|---:|---:|---|
| dev-scope | release | v_1.5.1 | 32 | 32 | 16 / 45 |  |
| dev-scope | release | v_1.5.6 | 65 | 65 | 16 / 45 |  |
| dev-scope | threshold |  | 28 | 28 | 16 / 45 |  |
| ktester | release | v_1.5.1 | 32 | 32 | 153 / 159 |  |
| ktester | release | v_1.5.6 | 65 | 65 | 153 / 159 |  |
| ouedraogo | release | v_1.5.1 | 32 | 32 | 47 / 169 |  |
| ouedraogo | release | v_1.5.6 | 65 | 65 | 47 / 169 |  |
| ouedraogo | threshold |  | 21 | 21 | 47 / 169 |  |

ktester: threshold never reached (or triggers disabled).

## Distinct pools

- `dev-scope/28` at `26b9106f96b3065205e7b3bda419976e76a1ec9a`
- `dev-scope/32` at `88fd31c639ba6cad5e8c11bf980690f04e27eb1e`
- `dev-scope/65` at `7812a55f3280fbb74076b2512a06dd34230a83f1`
- `ktester/32` at `88fd31c639ba6cad5e8c11bf980690f04e27eb1e`
- `ktester/65` at `7812a55f3280fbb74076b2512a06dd34230a83f1`
- `ouedraogo/21` at `b59f97e56fb5b5b41895bf29ecd8b3f5d4c311eb`
- `ouedraogo/32` at `88fd31c639ba6cad5e8c11bf980690f04e27eb1e`
- `ouedraogo/65` at `7812a55f3280fbb74076b2512a06dd34230a83f1`
