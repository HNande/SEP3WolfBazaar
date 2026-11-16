using BlazorApp.DTO;

namespace BlazorApp.Services;

public class LocalUserService : IUserService
{
    private readonly object _lock = new();
    private readonly List<StoredUser> _users = new();
    private int _nextId = 1;

    private record StoredUser(int Id, string FirstName, string LastName, string Name, string Email, string Password);

    public Task<UserDto> RegisterAsync(CreateUserDto dto)
    {
        var firstName = dto.FirstName?.Trim() ?? string.Empty;
        var lastName = dto.LastName?.Trim() ?? string.Empty;
        var name = dto.MemberName?.Trim() ?? string.Empty;
        var email = dto.MemberEmail?.Trim() ?? string.Empty;

        if (firstName.Length == 0)
            throw new ArgumentException("First name cannot be empty.");
        if (lastName.Length == 0)
            throw new ArgumentException("Last name cannot be empty.");
        if (name.Length == 0)
            throw new ArgumentException("User name cannot be empty.");
        if (email.Length == 0)
            throw new ArgumentException("Email cannot be empty.");
        if (string.IsNullOrEmpty(dto.Password))
            throw new ArgumentException("Password cannot be empty.");

        lock (_lock)
        {
            if (_users.Any(u => u.Name.Equals(name, StringComparison.OrdinalIgnoreCase)))
                throw new InvalidOperationException("This user name is already taken.");
            if (_users.Any(u => u.Email.Equals(email, StringComparison.OrdinalIgnoreCase)))
                throw new InvalidOperationException("This email is already taken.");

            var user = new StoredUser(_nextId++, firstName, lastName, name, email, dto.Password);
            _users.Add(user);
            return Task.FromResult(new UserDto
            {
                Id = user.Id,
                MemberName = user.Name,
                MemberEmail = user.Email
            });
        }
    }

    public Task<UserDto> LoginAsync(string userName, string password)
    {
        var name = userName?.Trim() ?? string.Empty;

        lock (_lock)
        {
            var user = _users.FirstOrDefault(u =>
                u.Name.Equals(name, StringComparison.OrdinalIgnoreCase) && u.Password == password);

            if (user == null)
                throw new InvalidOperationException("Wrong user name or password.");

            return Task.FromResult(new UserDto
            {
                Id = user.Id,
                MemberName = user.Name,
                MemberEmail = user.Email
            });
        }
    }
}