package com.neuedu.reggie.service;

import com.neuedu.reggie.entity.MenuImage;
import com.neuedu.reggie.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Component;

@Component
public class MenuImageValidator {

    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;

    public MenuImage validate(byte[] contents) {
        if (contents == null || contents.length == 0 || contents.length > MAX_BYTES) {
            throw invalidImage();
        }
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(contents))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if ((!format.equals("jpeg") && !format.equals("png"))
                        || reader.getWidth(0) > MAX_DIMENSION || reader.getHeight(0) > MAX_DIMENSION) {
                    throw invalidImage();
                }
                reader.read(0);
                return new MenuImage("image/" + format, contents);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BusinessException(BusinessException.Reason.INVALID_INPUT, imageRequirements(), exception);
        }
    }

    private static BusinessException invalidImage() {
        return new BusinessException(BusinessException.Reason.INVALID_INPUT, imageRequirements());
    }

    private static String imageRequirements() {
        return "请选择有效的 JPG 或 PNG 图片，文件不超过 5 MB，长宽不超过 4096 像素。";
    }
}
