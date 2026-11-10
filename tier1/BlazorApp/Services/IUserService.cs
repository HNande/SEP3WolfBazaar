using BlazorApp.DTO;

namespace BlazorApp.Services;

public interface IUserService
{
    Task<UserDto> RegisterAsync(CreateUserDto dto);
    Task<UserDto> LoginAsync(string userName, string password);
}
