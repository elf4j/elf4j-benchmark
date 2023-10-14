/*
 * MIT License
 *
 * Copyright (c) 2023 Easy Logging Facade for Java (ELF4J)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package elf4j.benchmark;

import ch.qos.logback.classic.LoggerContext;
import coco4j.MoreAwaitilities;
import elf4j.Logger;
import elf4j.engine.service.LogServiceManager;
import org.apache.logging.log4j.LogManager;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.LoggerFactory;
import org.tinylog.provider.ProviderRegistry;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@Warmup(iterations = 1, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(100)
@BenchmarkMode(Mode.Throughput)
@Fork(1)
public class LogBenchmark {
    private static final String LOG_MESSAGE_START = "Simple log message with counter: {}";
    private static final int CPU_TOKENS_PER_OP = 1_000_000;
    private static final int IO_BLOCK_PER_OP_MICROS = 20_000;
    static org.slf4j.Logger logbackLogger = LoggerFactory.getLogger(LogBenchmark.class.getName());
    static org.apache.logging.log4j.Logger log4jLogger = LogManager.getLogger(LogBenchmark.class);
    static Logger elf4jLogger = Logger.instance();
    private int i;

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder().include(LogBenchmark.class.getSimpleName()).shouldFailOnError(true).build();
        new Runner(opt).run();
    }

    private static void cpu(long tokens) {
        if (tokens == 0) {
            return;
        }
        Blackhole.consumeCPU(tokens);
    }

    private static void io(long blockMicros) {
        if (blockMicros == 0) {
            return;
        }
        MoreAwaitilities.sleepInterruptibly(Duration.of(blockMicros, ChronoUnit.MICROS));
    }

    private static void stopElf4J() {
        LogServiceManager.INSTANCE.shutdown();
    }

    private static void stopLog4J() {
        org.apache.logging.log4j.core.LoggerContext loggerContext =
                (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext();
        loggerContext.stop();
    }

    private static void stopLogback() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.stop();
    }

    private static void stopTinylog() {
        try {
            ProviderRegistry.getLoggingProvider().shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void workload() {
        cpu(CPU_TOKENS_PER_OP / 2);
        io(IO_BLOCK_PER_OP_MICROS);
        cpu(CPU_TOKENS_PER_OP / 2);
    }

    @Benchmark
    public int logback() {
        int o = ++i;
        logbackLogger.warn(LOG_MESSAGE_START, o);
        workload();
        return o;
    }

    @Benchmark
    public int log4j() {
        int o = ++i;
        log4jLogger.warn(LOG_MESSAGE_START, o);
        workload();
        return o;
    }

    @Benchmark
    public int noLog() {
        workload();
        return 0;
    }

    @Benchmark
    public int noWorkLoad() {
        return 0;
    }

    @Benchmark
    public int tinylog() {
        int o = ++i;
        org.tinylog.Logger.warn(LOG_MESSAGE_START, o);
        workload();
        return o;
    }

    @Benchmark
    public int elf4j() {
        int o = ++i;
        elf4jLogger.atWarn().log(LOG_MESSAGE_START, o);
        workload();
        return o;
    }

    @TearDown
    public void stopAll() {
        stopElf4J();
        stopLogback();
        stopLog4J();
        stopTinylog();
    }
}
