package dk.via.sep3.dto;

public record MemberResponseDTO(
    //This order is based on UserDto in t1
        int id,
        String memberName,
        String email,
        String firstName,
        String lastName
) {
}
