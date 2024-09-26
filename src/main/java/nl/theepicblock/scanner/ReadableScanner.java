package nl.theepicblock.scanner;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.regex.Pattern;

/**
 * Utility for reading from {@link Readable} inputs. Its primary function is
 * to buffer characters into a string for as long as a string matches a certain
 * {@link Pattern}, and returns the string afterward.
 * <p>
 * It serves a similar function to {@link java.util.Scanner}. But unlike
 * {@linkplain java.util.Scanner} it does not rely on delimiters.
 */
public class ReadableScanner {
    private final Readable input;

    private CharBuffer currentBuffer;
    private int bufferStart = 0;
    private int bufferEnd = 0;
    private boolean hitEof = false;

    public ReadableScanner(Readable input) {
        this(input, 128);
    }

    /**
     * Creates a {@link ReadableScanner} with a specific internal buffer size.
     */
    public ReadableScanner(Readable input, int capacity) {
        this.input = input;
        this.currentBuffer = CharBuffer.allocate(capacity);
    }

    /**
     * Will run the provided pattern <em>repeatedly</em> until no more characters can be matched.
     * @param p the pattern which is skipped
     */
    public void skip(Pattern p) throws IOException {
        var matcher = p.matcher(this.currentBuffer);

        while (true) {
            while (hasInput()) {
                // Look at the region that still needs to be read
                matcher.region(bufferStart, bufferEnd);
                if (matcher.lookingAt() && bufferStart != matcher.end()) {
                    bufferStart = matcher.end();
                } else {
                    // Nothing else could be matched
                    return;
                }
            }

            // Get more input
            clearBuffer();
            this.read();
            if (this.hitEof) {
                return;
            }
            matcher.reset(this.currentBuffer);
        }
    }

    /**
     * Will match the provided pattern <em>repeatedly</em> until no more characters can be matched.
     * Since this is run repeatedly, a pattern such as {@code "a{1}"} might actually return more
     * than one character. If this is undesirable, use {@link #read(Pattern)}
     * @param p the pattern which is matched
     * @param horizon the minimum amount of characters that must be present to run a match. The only scenario in
     *                which a match might be run without this amount of characters being present is when EOF has been
     *                hit. However, it is guaranteed that no new characters will be added after EOF, thus you can be
     *                certain that in all cases the pattern gets fully accurate information up until the horizon.
     * @return all characters that matched.
     */
    public String readRepeatedly(Pattern p, int horizon) throws IOException {
        var outputBuf = new StringBuilder();
        var matcher = p.matcher(this.currentBuffer);

        assert horizon <= currentBuffer.capacity();

        while (true) {
            if (currentBuffer.capacity() - bufferStart < horizon) {
                currentBuffer.position(bufferStart);
                currentBuffer.limit(bufferEnd);
                currentBuffer.compact();
                bufferEnd -= bufferStart;
                bufferStart = 0;
                currentBuffer.position(0);
                currentBuffer.limit(currentBuffer.capacity());
                matcher.reset(currentBuffer);
            }
            while (bufferEnd - bufferStart < horizon && !hitEof) {
                read();
            }
            do {
                matcher.region(bufferStart, bufferEnd);
                if (matcher.lookingAt() && bufferStart != matcher.end()) {
                    outputBuf.append(currentBuffer.slice(bufferStart, matcher.end()-bufferStart));
                    bufferStart = matcher.end();
                } else {
                    // There's nothing left to match
                    return outputBuf.toString();
                }
            } while (bufferEnd - bufferStart >= horizon);
            if (hitEof) {
                return outputBuf.toString();
            }
        }
    }

    /**
     * Matches a pattern on the readable's output until no more characters can be matched.
     * @param p the pattern which will be matched
     * @return all characters that match.
     */
    public String read(Pattern p) throws IOException {
        var matcher = p.matcher(this.currentBuffer);

        while (true) {
            matcher.region(bufferStart, bufferEnd);
            if (matcher.lookingAt() && (!matcher.hitEnd() || hitEof)) {
                var oldStart = bufferStart;
                bufferStart = matcher.end();
                return this.currentBuffer.slice(oldStart, matcher.end() - oldStart).toString();
            }

            if (this.currentBuffer.limit() == this.currentBuffer.capacity()) {
                var newBuffer = CharBuffer.allocate(this.currentBuffer.capacity() * 2);
                newBuffer.put(this.currentBuffer);
                newBuffer.position(0);
                newBuffer.limit(newBuffer.capacity());
                this.currentBuffer = newBuffer;
                matcher.reset(this.currentBuffer);
            }
            this.read();
        }
    }

    /**
     * Peeks a single byte from the readable.
     * @return the next character in the readable.
     */
    public char peek() throws IOException {
        if (this.hitEof && this.bufferStart == this.bufferEnd) {
            throw new IOException("Can't peek, hit end of stream");
        }
        if (!hasInput()) {
            this.read();
        }
        return this.currentBuffer.get(bufferStart);
    }

    /**
     * Pops a single byte from the readable.
     * @return the byte that was popped.
     */
    public char getChar() throws IOException {
        var c = peek();
        bufferStart++;
        return c;
    }

    /**
     * Checks if the scanner has hit the end of the readable.
     * @return true if the end has been reached, false otherwise
     */
    public boolean hasHitEof() throws IOException {
        if (!hasInput()) {
            this.read();
        }
        return (this.hitEof && this.bufferStart == this.bufferEnd);
    }

    /**
     * Reads from {@link #currentBuffer}.
     */
    private void read() throws IOException {
        currentBuffer.position(bufferEnd);
        var res = this.input.read(currentBuffer);
        if (res == -1) {
            this.hitEof = true;
        } else {
            bufferEnd += res;
        }
        currentBuffer.position(0);
    }

    /**
     * Checks if there's data in the {@link #currentBuffer} that still needs to be read.
     * @return true if there's data in the {@link #currentBuffer} that still needs to be read
     */
    private boolean hasInput() {
        return bufferStart < bufferEnd;
    }

    private void clearBuffer() {
        this.currentBuffer.position(0);
        this.currentBuffer.limit(this.currentBuffer.capacity());
        bufferStart = 0;
        bufferEnd = 0;
    }
}