module.exports = (req, res, next) => {
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
