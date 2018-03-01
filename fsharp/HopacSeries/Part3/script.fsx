#r "packages/Hopac/lib/net45/Hopac.Core.dll"
#r "packages/Hopac/lib/net45/Hopac.Platform.dll"
#r "packages/Hopac/lib/net45/Hopac.dll"
#r "packages/Hopac/lib/net45/Hopac.dll"
#r "packages/FSharp.Data/lib/net45/FSharp.Data.dll"
#r "packages/Http.fs/lib/net461/HttpFs.dll"
#r "packages/System.Net.Http/lib/net46/System.Net.Http.dll"

open Hopac
open FSharp.Data
open HttpFs.Client
open System

let httpGet url = job {
  let request = 
    Request.createUrl Get url 
    |> Request.setHeader (UserAgent "FsHopac")
  let! getResponseResult = 
    getResponse request |> Job.catch
  match getResponseResult with
  | Choice1Of2 response -> 
    match response.statusCode with 
    | 200 ->
      let! body = Response.readBodyAsString response
      return Ok body
    | _ -> return Error (Exception("invalid response for " + url))
  | Choice2Of2 ex -> return Error ex
} 
  

type GitHubUser = JsonProvider<"https://api.github.com/users/tamizhvendan">

type Profile = {
  Name : string
  AvatarUrl : string
}

let profile (gitHubUser : GitHubUser.Root) = {
  Name = gitHubUser.Name
  AvatarUrl = gitHubUser.AvatarUrl
}


let host = "https://api.github.com"
let userUrl = sprintf "%s/users/%s" host

let getGitHubProfile username = job {
  let! response = username |> userUrl |> httpGet 
  let user = 
    response
    |> Result.map GitHubUser.Parse
    |> Result.map profile
  return user
}


type GitHubRepos = JsonProvider<"https://api.github.com/users/tamizhvendan/repos">

type UserRepo = {
  Name : string
  StargazersCount : int
}
let userRepo (repo : GitHubRepos.Root) = {
  Name = repo.Name
  StargazersCount = repo.StargazersCount
}

let isOwnRepo (repo : GitHubRepos.Root) = not repo.Fork

let topThreeUserRepos (repos : GitHubRepos.Root []) =
  let takeCount =
    let reposCount = Array.length repos
    if reposCount > 3 then 3 else reposCount
  repos
  |> Array.filter isOwnRepo
  |> Array.map userRepo
  |> Array.sortByDescending (fun repo -> repo.StargazersCount)
  |> Array.take takeCount

let reposUrl = sprintf "%s/users/%s/repos" host

let getTopThreeUserRepos username = job {
  let! response = username |> reposUrl |> httpGet 
  let topThreeUserRepos = 
    response
    |> Result.map GitHubRepos.Parse
    |> Result.map topThreeUserRepos
  return topThreeUserRepos
}

let languagesUrl repoName userName = 
  sprintf "%s/repos/%s/%s/languages" host userName repoName

let parseLanguagesJson languagesJson =
  languagesJson
  |> JsonValue.Parse
  |> JsonExtensions.Properties
  |> Array.map fst

let getUserRepoLanguages repoName username = job {
  let! response = languagesUrl repoName username |> httpGet 
  let languages = 
    response
    |> Result.map parseLanguagesJson
  return languages
}

type UserRepoDto = {
  Name : string
  StargazersCount : int
  Languages : string []
}
let userRepoDto (userRepo : UserRepo) languagesResult = 
  languagesResult
  |> Result.map (fun languages -> {
                                    Name = userRepo.Name
                                    StargazersCount = userRepo.StargazersCount
                                    Languages = languages
                                  })

type ProfileDto = {
  Name : string
  AvatarUrl : string
  TopThreeRepos : UserRepoDto list
}

type ResultBuilder() = 
  member __.Bind(r, binder) = Result.bind binder r
  member __.Return(value) = Ok value

let result = ResultBuilder()

let rec transform (results : Result<'a, Exception> list) : Result<'a list, Exception> =
  let values = 
    results 
    |> List.choose (function | Ok v -> Some v | _ -> None)
  if values.Length = results.Length then 
    Ok values
  else 
    let ex =
      results 
      |> List.choose (function | Error ex -> Some ex | _ -> None)
      |> AggregateException 
    Error (ex :> Exception)


let profileDto (profileResult : Result<Profile,Exception>) userRepoDtosResults = result {
  let! profile = profileResult
  let! userRepoDtos = 
    transform userRepoDtosResults 
  let profileDto = {
    Name = profile.Name
    AvatarUrl = profile.AvatarUrl
    TopThreeRepos = userRepoDtos
  }
  return profileDto
} 


let getUserRepoLanguagesJobs username (repos : UserRepo []) =
  repos
  |> Array.map (fun repo -> 
                  getUserRepoLanguages repo.Name username 
                  |> Job.map (userRepoDto repo))
  |> Job.conCollect
  |> Job.map (fun x -> x.ToArray() |> Array.toList)

open Hopac.Infixes
let getProfileDto username = job {
  let! profile, topThreeUserRepos = 
    getGitHubProfile username <*> 
      getTopThreeUserRepos username
  let userRepoDtosJobResult =
    topThreeUserRepos
    |> Result.map (getUserRepoLanguagesJobs username)
  match userRepoDtosJobResult with
  | Ok userRepoDtosJob -> 
    let! userRepoDtos = userRepoDtosJob
    return profileDto profile userRepoDtos
  | Error ex -> return Error ex
}

getProfileDto "tamizhvendan" |> run
getProfileDto "demystifyfp" |> run