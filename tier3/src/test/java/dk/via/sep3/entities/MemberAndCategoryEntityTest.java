package dk.via.sep3.entities;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberAndCategoryEntityTest {

    @Test
    void memberConstructorSetsAllFields() {
        Member member = new Member("Ada", "Lovelace", "ada", "secret", "ada@example.com");

        assertThat(member.getFirstName()).isEqualTo("Ada");
        assertThat(member.getLastName()).isEqualTo("Lovelace");
        assertThat(member.getUsername()).isEqualTo("ada");
        assertThat(member.getPassword()).isEqualTo("secret");
        assertThat(member.getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void memberSettersUpdateFields() {
        Member member = new Member();

        member.setMemberId(3);
        member.setFirstName("Grace");
        member.setLastName("Hopper");
        member.setUsername("grace");
        member.setPassword("pw");
        member.setEmail("grace@example.com");

        assertThat(member.getMemberId()).isEqualTo(3);
        assertThat(member.getFirstName()).isEqualTo("Grace");
        assertThat(member.getLastName()).isEqualTo("Hopper");
        assertThat(member.getUsername()).isEqualTo("grace");
        assertThat(member.getPassword()).isEqualTo("pw");
        assertThat(member.getEmail()).isEqualTo("grace@example.com");
    }

    @Test
    void usernameIsUniqueAndRequired() throws NoSuchFieldException {
        Column column = Member.class.getDeclaredField("username").getAnnotation(Column.class);

        assertThat(column.unique()).isTrue();
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void categoryStoresItsNameAndNameIsUnique() throws NoSuchFieldException {
        Category category = new Category("CLOTHING");
        category.setName("FOOTWEAR");

        assertThat(category.getName()).isEqualTo("FOOTWEAR");
        assertThat(Category.class.getDeclaredField("name").getAnnotation(Column.class).unique()).isTrue();
    }
}
