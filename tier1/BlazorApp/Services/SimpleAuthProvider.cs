using System.Security.Claims;
using System.Text.Json;
using BlazorApp.DTO;
using BlazorApp.Services;
using Microsoft.AspNetCore.Components.Authorization;
using Microsoft.JSInterop;

namespace BlazorApp.Auth;

public class SimpleAuthProvider : AuthenticationStateProvider
{
    private const string StorageKey = "currentUser";

    private static readonly JsonSerializerOptions JsonOptions = new() { PropertyNameCaseInsensitive = true };

    private readonly IUserService userService;
    private readonly IJSRuntime js;

    public SimpleAuthProvider(IUserService userService, IJSRuntime js)
    {
        this.userService = userService;
        this.js = js;
    }

    public async Task Login(string userName, string password)
    {
        UserDto user = await userService.LoginAsync(userName, password);

        await js.InvokeVoidAsync("sessionStorage.setItem", StorageKey, JsonSerializer.Serialize(user));
        NotifyAuthenticationStateChanged(Task.FromResult(BuildState(user)));
    }

    public async Task Logout()
    {
        await js.InvokeVoidAsync("sessionStorage.removeItem", StorageKey);
        NotifyAuthenticationStateChanged(Task.FromResult(Anonymous()));
    }

    public override async Task<AuthenticationState> GetAuthenticationStateAsync()
    {
        string? json;
        try
        {
            json = await js.InvokeAsync<string?>("sessionStorage.getItem", StorageKey);
        }
        catch (InvalidOperationException)
        {
            return Anonymous();
        }

        if (string.IsNullOrEmpty(json))
            return Anonymous();

        try
        {
            UserDto? user = JsonSerializer.Deserialize<UserDto>(json, JsonOptions);
            return user == null ? Anonymous() : BuildState(user);
        }
        catch (JsonException)
        {
            return Anonymous();
        }
    }

    private static AuthenticationState BuildState(UserDto user)
    {
        var claims = new List<Claim>
        {
            new(ClaimTypes.Name, user.Name),
            new("Id", user.Id.ToString())
        };

        return new AuthenticationState(new ClaimsPrincipal(new ClaimsIdentity(claims, "apiauth")));
    }

    private static AuthenticationState Anonymous() => new(new ClaimsPrincipal(new ClaimsIdentity()));
}
