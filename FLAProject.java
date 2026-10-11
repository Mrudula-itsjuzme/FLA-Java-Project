import java.util.*;

/** Five Fundamental Languages and Automata algorithms. Java 8+. */
public class FLAProject {
    static final Scanner SC = new Scanner(System.in);
    static final String EPS = "ε";

    static class DFA {
        Set<String> states = new LinkedHashSet<>();
        Set<Character> alphabet = new LinkedHashSet<>();
        Map<String, Map<Character, String>> delta = new LinkedHashMap<>();
        String start;
        Set<String> finals = new LinkedHashSet<>();

        DFA(String start, Collection<Character> alphabet) {
            this.start = start;
            this.alphabet.addAll(alphabet);
            addState(start, false);
        }
        void addState(String s, boolean accepting) {
            states.add(s);
            if (accepting) finals.add(s);
        }
        void transition(String from, char symbol, String to) {
            if (!alphabet.contains(symbol)) throw new IllegalArgumentException("Unknown symbol " + symbol);
            states.add(from);
            states.add(to);
            delta.computeIfAbsent(from, k -> new LinkedHashMap<>()).put(symbol, to);
        }
        String next(String state, char symbol) {
            String result = delta.getOrDefault(state, Collections.emptyMap()).get(symbol);
            if (result == null) throw new IllegalArgumentException("Missing transition: " + state + ", " + symbol);
            return result;
        }
        boolean accepts(String input, boolean trace) {
            String current = start;
            if (trace) System.out.println("Start: " + current);
            for (char c : input.toCharArray()) {
                if (!alphabet.contains(c)) {
                    if (trace) System.out.println("Invalid symbol: " + c);
                    return false;
                }
                String next = next(current, c);
                if (trace) System.out.println(current + " --" + c + "--> " + next);
                current = next;
            }
            return finals.contains(current);
        }
        void print() {
            System.out.println("States: " + states + " | Alphabet: " + alphabet);
            System.out.println("Start: " + start + " | Final: " + finals);
            System.out.printf("%-24s", "State");
            for (char c : alphabet) System.out.printf("%-24s", c);
            System.out.println();
            for (String s : states) {
                System.out.printf("%-24s", s);
                for (char c : alphabet) System.out.printf("%-24s", delta.getOrDefault(s, Collections.emptyMap()).getOrDefault(c, "-"));
                System.out.println();
            }
        }
    }

    static class NFA {
        Set<String> states = new LinkedHashSet<>();
        Set<Character> alphabet = new LinkedHashSet<>();
        Map<String, Map<Character, Set<String>>> delta = new LinkedHashMap<>();
        Map<String, Set<String>> epsilon = new LinkedHashMap<>();
        String start;
        Set<String> finals = new LinkedHashSet<>();
        NFA(String start, Collection<Character> alphabet) {
            this.start = start;
            this.alphabet.addAll(alphabet);
            states.add(start);
        }
        void addState(String s, boolean accepting) {
            states.add(s);
            if (accepting) finals.add(s);
        }
        void transition(String from, char symbol, String to) {
            if (!alphabet.contains(symbol)) throw new IllegalArgumentException("Unknown symbol " + symbol);
            states.add(from); states.add(to);
            delta.computeIfAbsent(from, k -> new LinkedHashMap<>())
                .computeIfAbsent(symbol, k -> new LinkedHashSet<>()).add(to);
        }
        void epsilonTransition(String from, String to) {
            states.add(from); states.add(to);
            epsilon.computeIfAbsent(from, k -> new LinkedHashSet<>()).add(to);
        }
        Set<String> closure(Collection<String> input) {
            Set<String> result = new TreeSet<>(input);
            Deque<String> todo = new ArrayDeque<>(input);
            while (!todo.isEmpty()) {
                for (String dest : epsilon.getOrDefault(todo.remove(), Collections.emptySet())) {
                    if (result.add(dest)) todo.add(dest);
                }
            }
            return result;
        }
        Set<String> move(Set<String> from, char symbol) {
            Set<String> next = new TreeSet<>();
            for (String s : from)
                next.addAll(delta.getOrDefault(s, Collections.emptyMap())
                        .getOrDefault(symbol, Collections.emptySet()));
            return closure(next);
        }
        static String name(Set<String> subset) {
            return subset.isEmpty() ? "{}" : "{" + String.join(",", subset) + "}";
        }
        DFA toDFA() {
            Set<String> initial = closure(Collections.singleton(start));
            DFA dfa = new DFA(name(initial), alphabet);
            Map<String, Set<String>> seen = new LinkedHashMap<>();
            Queue<Set<String>> queue = new ArrayDeque<>();
            seen.put(name(initial), initial);
            queue.add(initial);
            while (!queue.isEmpty()) {
                Set<String> current = queue.remove();
                String from = name(current);
                dfa.addState(from, !Collections.disjoint(current, finals));
                for (char c : alphabet) {
                    Set<String> target = move(current, c);
                    String to = name(target);
                    dfa.transition(from, c, to);
                    if (!seen.containsKey(to)) {
                        seen.put(to, target);
                        queue.add(target);
                    }
                }
            }
            return dfa;
        }
    }

    // Partition refinement. First remove unreachable states, then split by transition signatures.
    static DFA minimize(DFA dfa) {
        Set<String> reachable = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        reachable.add(dfa.start); queue.add(dfa.start);
        while (!queue.isEmpty()) {
            String s = queue.remove();
            for (char c : dfa.alphabet) {
                String t = dfa.next(s, c);
                if (reachable.add(t)) queue.add(t);
            }
        }
        Set<String> accepting = new LinkedHashSet<>(reachable);
        accepting.retainAll(dfa.finals);
        Set<String> other = new LinkedHashSet<>(reachable);
        other.removeAll(accepting);
        List<Set<String>> blocks = new ArrayList<>();
        if (!accepting.isEmpty()) blocks.add(accepting);
        if (!other.isEmpty()) blocks.add(other);
        boolean changed;
        do {
            changed = false;
            Map<String, Integer> blockOf = new HashMap<>();
            for (int i = 0; i < blocks.size(); i++)
                for (String s : blocks.get(i)) blockOf.put(s, i);
            List<Set<String>> updated = new ArrayList<>();
            for (Set<String> block : blocks) {
                Map<List<Integer>, Set<String>> groups = new LinkedHashMap<>();
                for (String s : block) {
                    List<Integer> signature = new ArrayList<>();
                    for (char c : dfa.alphabet)
                        signature.add(blockOf.get(dfa.next(s, c)));
                    groups.computeIfAbsent(signature, k -> new LinkedHashSet<>()).add(s);
                }
                updated.addAll(groups.values());
                if (groups.size() > 1) changed = true;
            }
            blocks = updated;
        } while (changed);
        Map<String, String> renamed = new HashMap<>();
        DFA result = new DFA("TEMP", dfa.alphabet);
        result.states.clear();
        for (int i = 0; i < blocks.size(); i++) {
            String name = "M" + i;
            boolean isFinal = !Collections.disjoint(blocks.get(i), dfa.finals);
            result.addState(name, isFinal);
            for (String old : blocks.get(i)) renamed.put(old, name);
            System.out.println(name + " = " + blocks.get(i));
        }
        result.start = renamed.get(dfa.start);
        for (Set<String> block : blocks) {
            String representative = block.iterator().next();
            for (char c : dfa.alphabet)
                result.transition(renamed.get(representative), c, renamed.get(dfa.next(representative, c)));
        }
        return result;
    }

    static DFA intersection(DFA first, DFA second) {
        if (!first.alphabet.equals(second.alphabet))
            throw new IllegalArgumentException("DFAs must have the same alphabet");
        String start = pair(first.start, second.start);
        DFA result = new DFA(start, first.alphabet);
        Queue<List<String>> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        queue.add(Arrays.asList(first.start, second.start)); seen.add(start);
        while (!queue.isEmpty()) {
            List<String> state = queue.remove();
            String a = state.get(0), b = state.get(1), from = pair(a, b);
            result.addState(from, first.finals.contains(a) && second.finals.contains(b));
            for (char c : first.alphabet) {
                String x = first.next(a, c), y = second.next(b, c);
                String to = pair(x, y);
                result.transition(from, c, to);
                if (seen.add(to)) queue.add(Arrays.asList(x, y));
            }
        }
        return result;
    }
    static String pair(String a, String b) { return "(" + a + "," + b + ")"; }

    static class PDA {
        // A transition reads a character (null means epsilon), pops one stack
        // symbol (null means no pop), and pushes a string (leftmost on top).
        static class Rule {
            String from, to, push;
            Character read, pop;
            Rule(String from, Character read, Character pop, String to, String push) {
                this.from = from; this.read = read; this.pop = pop;
                this.to = to; this.push = push;
            }
        }
        static class Config {
            String state, stack;
            int position;
            Config(String state, int position, String stack) {
                this.state = state; this.position = position; this.stack = stack;
            }
            String key() { return state + "|" + position + "|" + stack; }
        }
        String start;
        Set<String> finals = new HashSet<>();
        List<Rule> rules = new ArrayList<>();
        String bottom = "$";
        PDA(String start) { this.start = start; }
        void addFinal(String state) { finals.add(state); }
        void addRule(String from, Character read, Character pop, String to, String push) {
            rules.add(new Rule(from, read, pop, to, push));
        }
        boolean accepts(String input, boolean trace) {
            Queue<Config> queue = new ArrayDeque<>();
            Set<String> seen = new HashSet<>();
            Config first = new Config(start, 0, bottom);
            queue.add(first); seen.add(first.key());
            // Finite search bounds prevent infinite epsilon loops on arbitrary PDAs.
            final int MAX_CONFIGS = 100000;
            final int MAX_STACK = Math.max(64, input.length() * 4 + 16);
            int processed = 0;
            while (!queue.isEmpty()) {
                if (++processed > MAX_CONFIGS) {
                    throw new IllegalStateException("PDA search limit reached; result inconclusive");
                }
                Config cur = queue.remove();
                if (trace) System.out.println("State=" + cur.state + ", pos=" + cur.position + ", stack=" + cur.stack);
                if (cur.position == input.length() && finals.contains(cur.state)) return true;
                for (Rule r : rules) {
                    if (!r.from.equals(cur.state)) continue;
                    if (r.read != null && (cur.position >= input.length() || input.charAt(cur.position) != r.read)) continue;
                    if (r.pop != null && (cur.stack.isEmpty() || cur.stack.charAt(0) != r.pop)) continue;
                    String rest = r.pop == null ? cur.stack : cur.stack.substring(1);
                    String nextStack = r.push + rest;
                    if (nextStack.length() > MAX_STACK) continue;
                    int nextPos = cur.position + (r.read == null ? 0 : 1);
                    Config next = new Config(r.to, nextPos, nextStack);
                    if (seen.add(next.key())) queue.add(next);
                }
            }
            return false;
        }
    }
    // Configured PDA for L = { 0^n 1^n | n >= 0 }.
    static PDA equalZerosOnesPDA() {
        PDA p = new PDA("push");
        p.addFinal("accept");
        p.addRule("push", '0', null, "push", "0");
        p.addRule("push", '1', '0', "pop", "");
        p.addRule("pop", '1', '0', "pop", "");
        p.addRule("push", null, '$', "accept", "$");
        p.addRule("pop", null, '$', "accept", "$");
        return p;
    }

    static List<Character> binary() { return Arrays.asList('0', '1'); }

    // DFA accepting strings ending in 01.
    static DFA ending01() {
        DFA d = new DFA("q0", binary());
        d.addState("q1", false); d.addState("q2", true);
        d.transition("q0", '0', "q1"); d.transition("q0", '1', "q0");
        d.transition("q1", '0', "q1"); d.transition("q1", '1', "q2");
        d.transition("q2", '0', "q1"); d.transition("q2", '1', "q0");
        return d;
    }
    // An NFA for strings ending in 01; q0 guesses where suffix begins.
    static NFA suffixNFA() {
        NFA n = new NFA("q0", binary());
        n.addState("q1", false); n.addState("q2", true);
        n.transition("q0", '0', "q0"); n.transition("q0", '1', "q0");
        n.transition("q0", '0', "q1"); n.transition("q1", '1', "q2");
        return n;
    }
    // Redundant-state DFA: A and B equivalent; unreachable U removed.
    static DFA redundantDFA() {
        DFA d = new DFA("S", binary());
        d.addState("A", true); d.addState("B", true); d.addState("U", false);
        d.transition("S", '0', "A"); d.transition("S", '1', "B");
        d.transition("A", '0', "A"); d.transition("A", '1', "B");
        d.transition("B", '0', "A"); d.transition("B", '1', "B");
        d.transition("U", '0', "U"); d.transition("U", '1', "U");
        return d;
    }
    // Even number of 0s.
    static DFA evenZeroes() {
        DFA d = new DFA("E", binary()); d.addState("E", true); d.addState("O", false);
        d.transition("E", '0', "O"); d.transition("E", '1', "E");
        d.transition("O", '0', "E"); d.transition("O", '1', "O");
        return d;
    }
    // Even number of 1s.
    static DFA evenOnes() {
        DFA d = new DFA("E", binary()); d.addState("E", true); d.addState("O", false);
        d.transition("E", '0', "E"); d.transition("E", '1', "O");
        d.transition("O", '0', "O"); d.transition("O", '1', "E");
        return d;
    }

    static String readInput() {
        System.out.print("Enter binary input string (press Enter for ε): ");
        return SC.nextLine().trim();
    }
    public static void main(String[] args) {
        while (true) {
            System.out.println("\n====== FLA JAVA PROJECT ======");
            System.out.println("1. DFA Simulation (ends with 01)");
            System.out.println("2. NFA to DFA (subset construction)");
            System.out.println("3. DFA Minimization (partition refinement)");
            System.out.println("4. Intersection of DFAs (even 0s AND even 1s)");
            System.out.println("5. PDA Simulation (0^n 1^n)");
            System.out.println("0. Exit");
            System.out.print("Choice: ");
            String choice = SC.nextLine().trim();
            switch (choice) {
                case "1": {
                    DFA d = ending01(); d.print();
                    System.out.println("Result: " + (d.accepts(readInput(), true) ? "ACCEPTED" : "REJECTED"));
                    break;
                }
                case "2": {
                    DFA d = suffixNFA().toDFA();
                    System.out.println("Equivalent DFA (reachable subsets):"); d.print();
                    System.out.println("Result: " + (d.accepts(readInput(), true) ? "ACCEPTED" : "REJECTED"));
                    break;
                }
                case "3": {
                    DFA d = redundantDFA();
                    System.out.println("Original DFA:"); d.print();
                    System.out.println("Equivalent groups after minimization:");
                    DFA reduced = minimize(d);
                    System.out.println("Minimized DFA:"); reduced.print();
                    System.out.println("Result: " + (reduced.accepts(readInput(), true) ? "ACCEPTED" : "REJECTED"));
                    break;
                }
                case "4": {
                    DFA d = intersection(evenZeroes(), evenOnes());
                    System.out.println("Intersection DFA:"); d.print();
                    System.out.println("Result: " + (d.accepts(readInput(), true) ? "ACCEPTED" : "REJECTED"));
                    break;
                }
                case "5": {
                    System.out.println("PDA recognizes 0^n1^n, n >= 0 (including ε).");
                    System.out.println("Result: " + (equalZerosOnesPDA().accepts(readInput(), true) ? "ACCEPTED" : "REJECTED"));
                    break;
                }
                case "0": System.out.println("Goodbye!"); return;
                default: System.out.println("Invalid choice. Choose 0-5.");
            }
        }
    }
}