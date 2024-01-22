import { NextFunction, Request, Response } from "express";

const mockAuthMiddleware = (
  req: Request,
  res: Response,
  next: NextFunction,
) => {
  const authHeader = req.headers.authorization;

  if (authHeader) {
    const token = authHeader.split(" ")[1];

    if (token === "VALID") {
      next();
      return;
    }
  }

  res.sendStatus(401);
};

export default mockAuthMiddleware;
