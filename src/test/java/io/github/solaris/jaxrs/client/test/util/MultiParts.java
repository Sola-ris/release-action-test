package io.github.solaris.jaxrs.client.test.util;

import static jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.READ;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;

import io.github.solaris.jaxrs.client.test.request.EntityConverter;
import io.github.solaris.jaxrs.client.test.request.RequestMatcher;

public final class MultiParts {
    public static final List<String> LIST_CONTENT = List.of("hello");
    public static final String PLAIN_CONTENT = "hello";

    private static final Path IMAGE_FILE;

    static {
        try {
            IMAGE_FILE = Files.createTempFile(MultiParts.class.getSimpleName(), ".png");

            BufferedImage image = new BufferedImage(400, 400, TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(WHITE);
            graphics.fillRect(0, 0, 400, 400);

            graphics.setColor(BLACK);
            graphics.fillOval(100, 100, 200, 200);

            ImageIO.write(image, "png", Files.newOutputStream(IMAGE_FILE, APPEND));

            IMAGE_FILE.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private MultiParts() {}

    public static EntityPart plainPart() throws IOException {
        return EntityPart.withName("plain")
                .header(CONTENT_LENGTH, String.valueOf(PLAIN_CONTENT.length()))
                .mediaType(TEXT_PLAIN_TYPE)
                .content(PLAIN_CONTENT)
                .build();
    }

    public static EntityPart jsonPart() throws IOException {
        return EntityPart.withName("json")
                .mediaType(APPLICATION_JSON_TYPE)
                .content(new Dto(false))
                .build();
    }

    public static EntityPart listPart() throws IOException {
        return EntityPart.withName("list")
                .mediaType(APPLICATION_JSON_TYPE.withCharset(UTF_8.name()))
                .content(LIST_CONTENT)
                .build();
    }

    public static EntityPart imagePart() throws IOException {
        return EntityPart.withFileName("image.png")
                .mediaType(new MediaType("image", "png"))
                .content(Files.newInputStream(IMAGE_FILE, READ))
                .build();
    }

    public static RequestMatcher partsBufferMatcher(List<EntityPart> expected, AtomicReference<PartsBuffer> target) {
        return request -> {
            EntityConverter converter = EntityConverter.fromRequestContext(request);
            target.set(new PartsBuffer(
                    converter.bufferExpectedMultipart(expected),
                    converter.bufferMultipartRequest(request)
            ));
        };
    }

    public record PartsBuffer(List<EntityPart> expected, List<EntityPart> actual) {}
}
