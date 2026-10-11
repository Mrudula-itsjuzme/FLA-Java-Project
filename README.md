# FLA Project: Five Automata Algorithms in Java

## Requirements
Java JDK 8 or newer. No external libraries.

## Run
Open a terminal in this folder and run:

```bash
javac FLAProject.java
java FLAProject
```

Select a menu option (1–5), enter a binary test string, and inspect the state-transition trace. Press Enter at the input prompt to use the empty string (epsilon). Choose 0 to exit.

## Algorithms and included examples

| Option | Task | Algorithm | Demonstration language |
|---|---|---|---|
| 1 | DFA simulation | Follow the unique transition for each input symbol | Binary strings ending in `01` |
| 2 | NFA to DFA | Reachable powerset/subset construction, with epsilon-closure support | Binary strings ending in `01` |
| 3 | DFA state reduction | Remove unreachable states and repeatedly refine final/nonfinal partitions | Merge equivalent accepting states A and B |
| 4 | DFA intersection | Reachable product states, accepting only when both component states accept | Binary strings with even numbers of both `0` and `1` |
| 5 | PDA simulation | Breadth-first exploration of configurations (state, input position, stack) | `0^n1^n`, n >= 0 |

## Tests

| Option | Input | Expected |
|---|---|---|
| 1 | `101` | ACCEPTED |
| 1 | `110` | REJECTED |
| 2 | `001` | ACCEPTED |
| 2 | `111` | REJECTED |
| 3 | `0` | ACCEPTED |
| 3 | empty | REJECTED |
| 4 | `0011` | ACCEPTED |
| 4 | `010` | REJECTED |
| 5 | `0011` | ACCEPTED |
| 5 | `0101` | REJECTED |
| 5 | empty | ACCEPTED |

## Customizing the automata

All core algorithms work on transition data, not hardcoded input outputs. Edit the factory methods near the bottom of `FLAProject.java` to define your own automata:

- `ending01()` supplies states, final states, alphabet, and transitions for DFA simulation.
- `suffixNFA()` uses `n.transition(...)` for nondeterministic transitions and optionally `n.epsilonTransition(...)` for epsilon moves. `toDFA()` performs the conversion.
- `redundantDFA()` supplies the DFA minimized by `minimize(...)`.
- `evenZeroes()` and `evenOnes()` supply the two DFAs for `intersection(...)`. Their alphabets must match.
- `equalZerosOnesPDA()` defines PDA transition rules. A rule is `(from, input character or null for epsilon, popped stack character or null for no pop, to, string to push)`. The leftmost pushed character becomes the stack top. `addFinal` specifies final-state acceptance.

The PDA simulator explores nondeterministic and epsilon transitions. A safety limit on the number of configurations and stack depth prevents an arbitrary PDA with unbounded epsilon pushes from running forever. If the search limit is hit, the program raises an **inconclusive** exception rather than falsely reporting rejection.

## Key theory for viva

1. **DFA:** One state at a time. Accept iff the final state after all input is accepting.
2. **NFA to DFA:** Each DFA state is a set of NFA states. The start subset is epsilon-closure of the NFA start state. Move on each symbol and take epsilon-closure again. A subset is accepting if it contains an NFA final state. The empty set is a valid dead state.
3. **Minimization:** Unreachable states cannot affect the language. Split reachable states into accepting/nonaccepting groups; repeatedly split groups whose transitions lead to different groups. Members of the final groups are equivalent.
4. **Intersection:** A state is an ordered pair `(q1,q2)`. Reading a symbol advances both DFAs. Only pairs whose **both** components are final are accepting.
5. **PDA:** Adds a stack to finite-state control. Here `0` pushes a symbol and `1` pops one. A string is accepted only after its entire input has been consumed and an accepting control state is reached.

## Complexity overview

- DFA simulation: `O(n)` for an input of length `n`.
- NFA to DFA: Worst-case `2^|Q|` subset states, where `Q` is the NFA state set.
- Minimization: This simple partition-refinement implementation is polynomial time, not the optimized Hopcroft algorithm.
- Intersection: At most `|Q1| × |Q2|` product states.
- PDA simulation: Configuration search can grow very large or be unbounded for arbitrary PDAs; this implementation uses finite search guards.