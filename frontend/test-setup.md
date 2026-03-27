# Frontend Setup Test Guide

## Issues Fixed

1. **Vite Base URL Configuration**: Changed from `/api/bugtracker/v1/` to `/` to prevent frontend from serving API routes
2. **Enhanced Error Handling**: Added comprehensive error handling for network issues, timeouts, and server errors
3. **API Timeout**: Added 10-second timeout to prevent long waits

## Test Commands

### 1. Start Backend First

```bash
cd backend
./gradlew bootRun
```

### 2. Start Frontend

```bash
cd frontend
npm run dev
```

### 3. Test API Connection

```bash
# Test if backend is running
curl -X GET http://localhost:8080/api/bugtracker/v1/auth/register

# Should return 405 Method Not Allowed (which is correct for GET on POST endpoint)
```

### 4. Test Frontend Routes

- Navigate to `http://localhost:5173` - should show landing page
- Navigate to `http://localhost:5173/auth/login` - should show login form
- Navigate to `http://localhost:5173/auth/register` - should show registration form

## Expected Behavior

### When Backend is Running:

- Login/Register forms should show proper error messages for invalid credentials
- Network errors should be handled gracefully with toast notifications

### When Backend is Not Running:

- Login/Register attempts should show: "Unable to connect to the server. Please check if the backend is running."
- No more 404 errors in browser navigation

## Troubleshooting

If you still see issues:

1. **Clear Browser Cache**: Hard refresh (Ctrl+F5) or clear cache
2. **Check Backend Status**: Ensure backend is running on port 8080
3. **Check Network Tab**: Look for failed requests to `localhost:8080`
4. **Check Console**: Look for any JavaScript errors

## Next Steps

1. Create a test user via registration
2. Test login with the created user
3. Verify JWT token is stored in localStorage
4. Test protected routes
