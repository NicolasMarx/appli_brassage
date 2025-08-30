module Route exposing (Route(..), fromUrl, href)

import Url exposing (Url)
import Url.Parser as P exposing ((</>), Parser, s, oneOf)

type Route
    = RLogin
    | RHome
    | RHops
    | RUpload
    | RReferentials

parser : Parser (Route -> a) a
parser =
    oneOf
        [ P.map RLogin (s "admin" </> s "login")
        , P.map RHome (s "admin")
        , P.map RHops (s "admin" </> s "hops")
        , P.map RUpload (s "admin" </> s "upload")
        , P.map RReferentials (s "admin" </> s "referentials")
        ]

fromUrl : Url -> Maybe Route
fromUrl =
    P.parse parser

href : Route -> String
href route =
    case route of
        RLogin -> "/admin/login"
        RHome -> "/admin"
        RHops -> "/admin/hops"
        RUpload -> "/admin/upload"
        RReferentials -> "/admin/referentials"
