package coverosR3z.authentication

import coverosR3z.server.*

fun doGETLogout(au: IAuthenticationUtilities, rd: RequestData): PreparedResponseData {
    return if (isAuthenticated(rd)) {
        au.logout(rd.sessionToken)
        okHTML(logoutHTML)
    } else {
        redirectTo(NamedPaths.HOMEPAGE.path)
    }
}

const val logoutHTML = """
<!DOCTYPE html>    
<html>
    <head>
        <title>Logout</title>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <link rel="stylesheet" href="general.css" />
    </head>
    <body>
            <p>
                You are now logged out
            </p>

            <p><a href="homepage">Homepage</a></p>
    </body>
</html>    
"""