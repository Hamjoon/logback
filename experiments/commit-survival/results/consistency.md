# Consistency checks

```json
{
  "nonincreasing": true,
  "index_zero_equals_denominators": true,
  "raw_records": 288,
  "expected_raw_records": 288,
  "raw_count_matches": true,
  "missing_records": [],
  "build_fail_indices": [
    11,
    68,
    70,
    79
  ],
  "measured_raw_records": 288,
  "derived_raw_records": 0,
  "expected_measured_raw_records": 288,
  "expected_derived_raw_records": 0,
  "skipped_indices": [
    9,
    14,
    23,
    27,
    34,
    42,
    45,
    46,
    47,
    50,
    51,
    56,
    59,
    67,
    69,
    72,
    74,
    75,
    78,
    82,
    89,
    94,
    96
  ]
}
```

Coverage: identical executable line sets across all 3 groups at 57 indices. Required indices: 60. Missing measurements: 9 (enumerated in coverage-consistency.json).

## Three-group checks

| Check | Dev (scope) | KTester | Ouédraogo |
|---|---:|---:|---:|
| Fixed denominator | 45 | 159 | 169 |
| Final survivors | 10 | 145 | 47 |
| Survival records | 96 | 96 | 96 |
| Coverage records | 74 | 74 | 74 |
| Matching executable universes | 57 | 57 | 57 |
