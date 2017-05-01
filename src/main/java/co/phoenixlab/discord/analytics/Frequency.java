package co.phoenixlab.discord.analytics;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Frequency {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final Pattern DATE = Pattern.compile("[0-1][0-9]/[0-3][0-9]/[0-9][0-9]");

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter log dir:");
        Path dir = Paths.get(scanner.nextLine());
        System.out.printf("Scanning %s...%n", dir.toString());
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile).forEach(Frequency::processLog);
        }
    }

    private static void processLog(Path path) {
        List<String> lines = new ArrayList<>(1000000);
        getLines(path,  lines);
        long startTime = System.nanoTime();
        Results results = new Results();
        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.invoke(new ForkCount(lines, results));
        long endTime = System.nanoTime();
        System.out.println("============================================================");
        System.out.printf("Total tallys for %s:\n", path.getFileName().toString());
        SortedMap<String, LongAdder> ret = results.getDays();
        LocalDate lastZdt = null;
        for (Map.Entry<String, LongAdder> entry : ret.entrySet()) {
            //  Insert empty days in as necessary
            if (lastZdt == null) {
                lastZdt = LocalDate.parse(entry.getKey(), FORMATTER);
            } else {
                LocalDate zdt = LocalDate.parse(entry.getKey(), FORMATTER);
                Duration delta = Duration.between(zdt.atStartOfDay(), lastZdt.atStartOfDay()).abs();
                while (delta.compareTo(Duration.ofDays(1)) > 0  && !zdt.isEqual(lastZdt)) {
                    lastZdt = lastZdt.plusDays(1);
                    System.out.printf("%s\t%s%n", FORMATTER.format(lastZdt), 0);
                    delta = Duration.between(zdt.atStartOfDay(), lastZdt.atStartOfDay()).abs();
                }
                lastZdt = zdt;
            }
            System.out.printf("%s\t%s%n", entry.getKey(), entry.getValue().longValue());
        }
        long delta = endTime - startTime;
        System.out.printf("Processed in %,d nanos (%,d millis)\n", delta, TimeUnit.NANOSECONDS.toMillis(delta));
        System.out.println("============================================================");
    }

    private static void getLines(Path path, List<String> ret) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() < 8) {
                    continue;
                }
                if (DATE.asPredicate().test(line.substring(0, 8))) {
                    ret.add(line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    static class ForkCount extends RecursiveAction {

        final Map<String, LongAdd> days = new HashMap<>();
        private final List<String> lines;
        private final Results results;

        ForkCount(List<String> lines, Results results) {
            this.lines = lines;
            this.results = results;
        }

        @Override
        protected void compute() {
            if (lines.size() < 4096) {
                lines.stream().map(s -> s.substring(0, 8)).
                        forEach(this::count);
                days.forEach((key, value) -> results.accumulate(key, value.value));
            } else {
                int splitSize = lines.size() / 2;
                invokeAll(new ForkCount(lines.subList(0, splitSize), results),
                        new ForkCount(lines.subList(splitSize, lines.size()), results));
            }
        }

        private void count(String date) {
            LongAdd add = days.computeIfAbsent(date, k -> new LongAdd());
            add.increment();
        }

    }

    static class LongAdd {
        public long value;

        void increment() {
            ++value;
        }
    }

}
