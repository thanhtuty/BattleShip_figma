IF DB_ID(N'BattleShipDB') IS NULL
BEGIN
    CREATE DATABASE BattleShipDB;
END
GO

USE BattleShipDB;
GO

IF OBJECT_ID('dbo.Users', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Users (
        UserId       INT IDENTITY(1,1) PRIMARY KEY,
        Username     NVARCHAR(40) NOT NULL UNIQUE,
        Email        NVARCHAR(120) NULL UNIQUE,
        PasswordHash CHAR(64) NOT NULL,
        PasswordSalt CHAR(32) NOT NULL,
        Wins         INT NOT NULL DEFAULT 0,
        Losses       INT NOT NULL DEFAULT 0,
        Rating       INT NOT NULL DEFAULT 1000,
        CreatedAt    DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );
END
GO

IF OBJECT_ID('dbo.Matches', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.Matches (
        MatchId      BIGINT IDENTITY(1000,1) PRIMARY KEY,
        Player1Id    INT NOT NULL,
        Player2Id    INT NOT NULL,
        WinnerId     INT NULL,
        Status       VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
        StartedAt    DATETIME2 NULL,
        FinishedAt   DATETIME2 NULL,
        CreatedAt    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Matches_P1 FOREIGN KEY(Player1Id) REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_Matches_P2 FOREIGN KEY(Player2Id) REFERENCES dbo.Users(UserId),
        CONSTRAINT FK_Matches_W FOREIGN KEY(WinnerId) REFERENCES dbo.Users(UserId)
    );
END
GO

IF OBJECT_ID('dbo.MatchEvents', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.MatchEvents (
        EventId      BIGINT IDENTITY(1,1) PRIMARY KEY,
        MatchId      BIGINT NOT NULL,
        ActorUserId  INT NULL,
        EventType    VARCHAR(30) NOT NULL,
        RowNo        INT NULL,
        ColNo        INT NULL,
        EventValue   NVARCHAR(200) NULL,
        CreatedAt    DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        CONSTRAINT FK_Events_Match FOREIGN KEY(MatchId) REFERENCES dbo.Matches(MatchId),
        CONSTRAINT FK_Events_User FOREIGN KEY(ActorUserId) REFERENCES dbo.Users(UserId)
    );
END
GO

CREATE OR ALTER VIEW dbo.vw_Ranking AS
SELECT
    UserId,
    Username,
    Wins,
    Losses,
    Rating,
    CASE WHEN Wins + Losses = 0 THEN 0
         ELSE CAST(Wins * 100.0 / (Wins + Losses) AS DECIMAL(5,2))
    END AS WinRate
FROM dbo.Users;
GO
