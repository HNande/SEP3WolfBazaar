using System.Net;
using System.Text.Json;
using BlazorApp.DTO;

namespace BlazorApp.Services;

public class HttpUserService : IUserService
{
    private readonly HttpClient _http;

    public HttpUserService(HttpClient httpClient)
    {
        _http = httpClient;
    }

    private record RegisteredMember(string FirstName, string LastName, string Username);

    public async Task<UserDto> RegisterAsync(CreateUserDto dto)
        {
            var response = await _http.PostAsJsonAsync("api/member", new
            {
                firstName = dto.FirstName,
                lastName = dto.LastName,
                username = dto.Name,
                password = dto.Password
            });
            if (!response.IsSuccessStatusCode)
                throw new Exception(await ReadErrorAsync(response));

            var member = await response.Content.ReadFromJsonAsync<RegisteredMember>()
                         ?? throw new InvalidOperationException("Failed to deserialize member response.");

            return new UserDto { Name = member.Username };
        }

    public async Task<UserDto> LoginAsync(string userName, string password)
        {
            var response = await _http.PostAsJsonAsync("api/auth/login", new { username = userName, password });

            if (response.StatusCode == HttpStatusCode.Unauthorized)
                throw new Exception("Wrong user name or password.");
            if (!response.IsSuccessStatusCode)
                throw new Exception(await ReadErrorAsync(response));

            return await response.Content.ReadFromJsonAsync<UserDto>()
                   ?? throw new InvalidOperationException("Failed to deserialize user response.");
        }

        private static async Task<string> ReadErrorAsync(HttpResponseMessage response)
        {
            try
            {
                var error = await response.Content.ReadFromJsonAsync<ApiErrorDto>();
                if (!string.IsNullOrWhiteSpace(error?.Message))
                    return error.Message;
            }
            catch (Exception ex) when (ex is JsonException or NotSupportedException)
            {
            }

            return $"Request failed (HTTP {(int)response.StatusCode} {response.ReasonPhrase})";
        }
    }