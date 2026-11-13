namespace BlazorApp.DTO;

public class CreateUserDto
{
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string Name { get; set; } = string.Empty; //username
    public string Password { get; set; } = string.Empty;
}
