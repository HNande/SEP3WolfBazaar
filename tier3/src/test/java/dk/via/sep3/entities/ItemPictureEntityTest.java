package dk.via.sep3.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItemPictureEntityTest {

    @Test
    void threeArgumentConstructorLeavesThumbnailEmpty() {
        ItemPicture picture = new ItemPicture("a.png", ImageFileType.PNG, "/img/a.png");

        assertThat(picture.getFileName()).isEqualTo("a.png");
        assertThat(picture.getFileType()).isEqualTo(ImageFileType.PNG);
        assertThat(picture.getFilePath()).isEqualTo("/img/a.png");
        assertThat(picture.getThumbnailPath()).isNull();
    }

    @Test
    void fourArgumentConstructorStoresThumbnailPath() {
        ItemPicture picture = new ItemPicture("a.jpg", ImageFileType.JPG, "/img/a.jpg", "/img/a_thumb.jpg");

        assertThat(picture.getThumbnailPath()).isEqualTo("/img/a_thumb.jpg");
    }

    @Test
    void settersUpdateFields() {
        ItemPicture picture = new ItemPicture("a.png", ImageFileType.PNG, "/a");

        picture.setFileName("b.jpg");
        picture.setFileType(ImageFileType.JPEG);
        picture.setFilePath("/b");
        picture.setThumbnailPath("/b_thumb");

        assertThat(picture.getFileName()).isEqualTo("b.jpg");
        assertThat(picture.getFileType()).isEqualTo(ImageFileType.JPEG);
        assertThat(picture.getFilePath()).isEqualTo("/b");
        assertThat(picture.getThumbnailPath()).isEqualTo("/b_thumb");
    }
}
